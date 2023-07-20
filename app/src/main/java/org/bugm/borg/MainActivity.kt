package org.bugm.borg

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Parcel
import android.os.Parcelable
import android.provider.OpenableColumns
import android.provider.Settings
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.bugm.borg.AppInitialization.PREFS_NAME
import org.bugm.borg.AppInitialization.PREFS_SELECTED_FILES
import java.io.File

data class SelectedFile(
    val uriString: String, val name: String, var selectedForRemoval: Boolean = false
) : Parcelable {
    // Constructor for parceling
    constructor(parcel: Parcel) : this(
        parcel.readString() ?: "", parcel.readString() ?: "", parcel.readInt() != 0
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(uriString)
        parcel.writeString(name)
        parcel.writeInt(if (selectedForRemoval) 1 else 0)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<SelectedFile> {
        override fun createFromParcel(parcel: Parcel): SelectedFile {
            return SelectedFile(parcel)
        }

        override fun newArray(size: Int): Array<SelectedFile?> {
            return arrayOfNulls(size)
        }
    }
}

class MainActivity : AppCompatActivity() {

    private lateinit var selectedFiles: MutableList<SelectedFile>
    private lateinit var fileAdapter: FileAdapter

    private val requestOverlayPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val foo = "bar"
        } else {
            // Permission denied, handle accordingly (show a message, etc.)
        }
    }

    private fun updateServiceWithSelectedFiles(selectedFiles: List<SelectedFile>) {
        // Save the latest selected files to SharedPreferences
        saveSelectedFilesToSharedPreferences(this, selectedFiles)

        // Update the service with the latest selected files (if needed)
        ScreenUnlockForegroundService.startService(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Check and request the SYSTEM_ALERT_WINDOW permission on devices with Android 12 (API 31) and above
        requestOverlayPermission()

        // Init file adapter and recycler view for selected files
        selectedFiles = getSelectedFilesFromSharedPreferences(this)
        fileAdapter = FileAdapter(selectedFiles)
        val recyclerView = findViewById<RecyclerView>(R.id.fileRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = fileAdapter

        val filePickerLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == Activity.RESULT_OK) {
                    val data = result.data
                    data?.let {
                        val clipData = it.clipData
                        if (clipData != null) {
                            for (i in 0 until clipData.itemCount) {
                                val fileUri = clipData.getItemAt(i).uri
                                val fileName = getFileName(fileUri)
                                if (!fileAlreadyExists(fileName)) {
                                    selectedFiles.add(SelectedFile(fileUri.toString(), fileName))
                                }
                            }
                        } else {
                            val uri = it.data
                            uri?.let { it1 ->
                                val fileName = getFileName(it1)
                                if (!fileAlreadyExists(fileName)) {
                                    selectedFiles.add(SelectedFile(it1.toString(), fileName))
                                }
                            }
                        }
                        fileAdapter.notifyDataSetChanged()

                        // Save the selected files directly to SharedPreferences
                        saveSelectedFilesToSharedPreferences(this, selectedFiles)

                        // Update the service with the latest selected files when a file is added
                        ScreenUnlockForegroundService.startService(this)
                    }
                }
            }

        val pickIntent = Intent(Intent.ACTION_GET_CONTENT)
        pickIntent.type = "text/*"
        pickIntent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)

        findViewById<View>(R.id.selectFilesButton).setOnClickListener {
            filePickerLauncher.launch(Intent.createChooser(pickIntent, "Select Files"))
        }

    }

    private fun fileAlreadyExists(fileName: String): Boolean {
        for (file in selectedFiles) {
            if (file.name == fileName) {
                return true
            }
        }
        return false
    }

    private fun requestOverlayPermission() {
        if (!Settings.canDrawOverlays(this)) {
            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
            intent.data = Uri.parse("package:$packageName")
            requestOverlayPermissionLauncher.launch(intent)
        } else {
            ScreenUnlockForegroundService.startService(this)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Save selected files to SharedPreferences when the activity is destroyed
        val selectedFilesJson = Gson().toJson(selectedFiles)
        val editor = getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit()
        editor.putString(PREFS_SELECTED_FILES, selectedFilesJson)
        editor.apply()
    }

    private fun getFileName(uri: Uri): String {
        when (uri.scheme) {
            "content" -> {
                val cursor = contentResolver.query(uri, null, null, null, null)
                cursor?.use {
                    if (it.moveToFirst()) {
                        val displayNameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (displayNameIndex != -1) {
                            return it.getString(displayNameIndex)
                        }
                    }
                }
            }

            "file" -> {
                return File(uri.path.toString()).name
            }
        }
        return ""
    }

}
