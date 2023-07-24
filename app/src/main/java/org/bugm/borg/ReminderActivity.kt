package org.bugm.borg

import android.content.ContentValues.TAG
import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Random

data class TaskItem(
    val description: String, val priority: String?, val tags: List<String>, val dueDate: String?
)

class ReminderActivity : AppCompatActivity(), CoroutineScope by MainScope() {

    private val coroutineScope = CoroutineScope(Dispatchers.Main)
    private lateinit var uris: ArrayList<Uri>
    private val taskItems = mutableListOf<TaskItem>()

    private val requestReadPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                // Permission granted, proceed with reading the files
                val data: Intent? = result.data
                data?.data?.let { uri ->
                    coroutineScope.launch {
                        parseFileContent(uri)
                    }
                }
            } else {
                // Permission denied or user canceled the permission request, handle the case appropriately
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reminder)

        // selectedFiles = getSelectedFilesFromSharedPreferences(this)
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
        coroutineScope.launch {
            parseSelectedFiles()
        }.join()
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
        var collectedItems: List<TaskItem>
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
        val priorityPattern = Regex("\\[([A-Z])\\]")
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

    private fun pickRandomFile(selectedFiles: List<SelectedFile>): SelectedFile? {
        return if (selectedFiles.isNotEmpty()) {
            val randomIndex = Random().nextInt(selectedFiles.size)
            selectedFiles[randomIndex]
        } else {
            null
        }
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

    private fun requestReadPermission(uri: Uri) {
        val intent =
            Intent(Intent.ACTION_OPEN_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE).setType("*/*")
                .putExtra(Intent.EXTRA_ALLOW_MULTIPLE, false)

        requestReadPermissionLauncher.launch(intent)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, proceed with reading the files
                // You can call the method to read the files here or handle it in the permission launcher result
            } else {
                // Permission denied, handle the case appropriately
            }
        }
    }

    companion object {
        private const val PERMISSION_REQUEST_CODE = 100
    }

}
