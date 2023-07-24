package org.bugm.borg

import android.content.ContentValues.TAG
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.datatransport.runtime.firebase.transport.TimeWindow
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.bugm.borg.AppInitialization.PREFS_TASK_ITEMS
import org.bugm.borg.AppInitialization.PREFS_TASK_ITEMS_TIMESTAMP
import java.util.Random
import java.util.concurrent.TimeUnit

data class TaskItem(
    val description: String, val priority: String?, val tags: List<String>, val dueDate: String?
)

data class ContextTimeWindow(
    val contextName: String,
    val fromHour: Int,
    val fromMinute: Int,
    val toHour: Int,
    val toMinute: Int
)

class ReminderActivity : AppCompatActivity(), CoroutineScope by MainScope() {

    private val coroutineScope = CoroutineScope(Dispatchers.Main)
    private lateinit var uris: ArrayList<Uri>
    private val taskItems = mutableListOf<TaskItem>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reminder)

        uris = intent.getParcelableArrayListExtra<Uri>("uris", Uri::class.java)
            ?.filterNotNull() as ArrayList<Uri>

        val btnClose = findViewById<Button>(R.id.btnClose)
        btnClose.setOnClickListener {
            finish()
        }

        coroutineScope.launch {
            setReminderText()
        }
    }

    private suspend fun setReminderText() {
        val prefs = getSharedPreferences(PREFS_TASK_ITEMS, Context.MODE_PRIVATE)

        // Retrieve cached task items JSON and timestamp from SharedPreferences
        val cachedTaskItemsJson = prefs.getString(PREFS_TASK_ITEMS, null)
        val cachedTaskItemsTimestamp = prefs.getLong(PREFS_TASK_ITEMS_TIMESTAMP, 0)

        // Check if the cached data is available and not expired (within 30 minutes)
        val currentTimeMillis = System.currentTimeMillis()
        val expirationTimeMillis = TimeUnit.MINUTES.toMillis(30)
        if (!cachedTaskItemsJson.isNullOrEmpty() && (currentTimeMillis - cachedTaskItemsTimestamp) < expirationTimeMillis) {
            // Cached data is still valid, parse the JSON and update taskItems
            val listType = object : TypeToken<List<TaskItem>>() {}.type
            val cachedTaskItems = Gson().fromJson<List<TaskItem>>(cachedTaskItemsJson, listType)
            taskItems.clear()
            taskItems.addAll(cachedTaskItems)
            Log.d(TAG, "Using " + taskItems.size.toString() + " cached task items")
        } else {
            // Cached data has expired or doesn't exist, parse the files and update taskItems
            parseSelectedFiles()

            // Cache the parsed task items along with the current timestamp
            val taskItemsJson = Gson().toJson(taskItems)
            prefs.edit().putString(PREFS_TASK_ITEMS, taskItemsJson)
                .putLong(PREFS_TASK_ITEMS_TIMESTAMP, currentTimeMillis).apply()

            Log.d(TAG, "Parsed and cached " + taskItems.size.toString() + " task items")
        }

        val reminderText = findViewById<TextView>(R.id.reminderTextView)
        reminderText.text = pickRandomItem(taskItems)
    }

    private suspend fun parseSelectedFiles() {
        for (uri in uris) {
            try {
                parseFileContent(uri)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private suspend fun parseFileContent(uri: Uri) = withContext(Dispatchers.IO) {
        val contentResolver = contentResolver
        val inputStream = contentResolver.openInputStream(uri)
        val orgFileContent = inputStream?.bufferedReader()?.use { it.readText() }
        inputStream?.close()

        if (orgFileContent != null) {
            val taskPattern = Regex("(?<=^|\\n)\\*+\\s+TODO\\s+(.*?)(?=\\n|$)")
            val matches = taskPattern.findAll(orgFileContent)
            for (match in matches) {
                val todoText = match.groups[1]?.value?.trim()
                if (!todoText.isNullOrEmpty()) {
                    val priority = parsePriority(todoText)
                    val tags = parseTags(todoText)
                    val dueDate = parseDueDate(todoText)
                    val taskItem = TaskItem(todoText, priority, tags, dueDate)
                    taskItems.add(taskItem)

                    // Log the parsed task item for debugging
                    Log.d(TAG, "Parsed Task Item: $taskItem")
                }
            }
        } else {
            // Log an error message to check if the content is null
            Log.e(TAG, "Content is null for URI: $uri")
        }
    }

    private fun parsePriority(line: String): String? {
        val priorityPattern = Regex("\\[([A-Z])]")
        val match = priorityPattern.find(line)
        return match?.groupValues?.get(1)
    }

    private fun parseTags(line: String): List<String> {
        val tagPattern = Regex("(?<=:)(\\w+)(?=:)")
        val matches = tagPattern.findAll(line)
        return matches.map { it.value }.toList()
    }

    private fun parseDueDate(line: String): String? {
        // Define your custom parsing logic here to extract the due date from the line.
        // For example, if the due date is represented as "DEADLINE: yyyy-mm-dd", you can use:
        // val dueDatePattern = Regex("DEADLINE: (\\d{4}-\\d{2}-\\d{2})")
        // val match = dueDatePattern.find(line)
        // return match?.groupValues?.get(1)
        return null
    }

    private fun pickRandomItem(taskItems: List<TaskItem>): String? {
        return if (taskItems.isNotEmpty()) {
            val randomIndex = Random().nextInt(taskItems.size)
            taskItems[randomIndex].description
        } else {
            null
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Cancel the coroutine when the activity is destroyed to avoid leaks
        coroutineScope.cancel()
    }

}
