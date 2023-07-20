package org.bugm.borg

import android.content.ContentValues.TAG
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.bugm.borg.AppInitialization.PREFS_SELECTED_FILES
import java.util.Random

class ReminderActivity : AppCompatActivity() {

    private val coroutineScope = CoroutineScope(Dispatchers.Main)
    private lateinit var selectedFiles: MutableList<SelectedFile>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reminder)

        selectedFiles = getSelectedFilesFromSharedPreferences(this)

        val btnClose = findViewById<Button>(R.id.btnClose)
        btnClose.setOnClickListener {
            finish()
        }

        val reminderText = findViewById<TextView>(R.id.reminderTextView)
        val randomFile = pickRandomFile(selectedFiles)
        reminderText.text = randomFile?.name

        for (file in selectedFiles) {
            Log.d(TAG, file.name)
            val uriString = file.uriString
            val name = file.name

        }

        coroutineScope.launch {
            for (headline in mutableListOf("apple", "banana", "orange")) {
                val title = headline
            }
        }
    }

    private fun pickRandomFile(selectedFiles: List<SelectedFile>): SelectedFile? {
        return if (selectedFiles.isNotEmpty()) {
            val randomIndex = Random().nextInt(selectedFiles.size)
            selectedFiles[randomIndex]
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
