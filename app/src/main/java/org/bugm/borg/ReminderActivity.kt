package org.bugm.borg

import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.bugm.borg.AppInitialization.PREFS_SELECTED_FILES
import java.io.File

class ReminderActivity : AppCompatActivity() {

    private val coroutineScope = CoroutineScope(Dispatchers.Main)
    private lateinit var selectedFiles: MutableList<SelectedFile>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reminder)

        // Retrieve the list of selected files from the intent extras
        selectedFiles = intent?.getParcelableArrayListExtra(
            PREFS_SELECTED_FILES, SelectedFile::class.java
        ) ?: mutableListOf()


        val btnClose = findViewById<Button>(R.id.btnClose)
        btnClose.setOnClickListener {
            finish()
        }

        coroutineScope.launch {
            for (headline in mutableListOf("apple", "banana", "orange")) {
                val title = headline
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Cancel the coroutine when the activity is destroyed to avoid leaks
        coroutineScope.cancel()
    }

}
