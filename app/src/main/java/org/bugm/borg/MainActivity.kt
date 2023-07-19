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
            // Permission granted, start the service
            startScreenUnlockService()
        } else {
            // Permission denied, handle accordingly (show a message, etc.)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        selectedFiles = mutableListOf()

        // Check and request the SYSTEM_ALERT_WINDOW permission on devices with Android 12 (API 31) and above
        requestOverlayPermission()

        // Init file adapter and recycler view for selected files
        fileAdapter = FileAdapter(selectedFiles)
        val recyclerView = findViewById<RecyclerView>(R.id.fileRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = fileAdapter

        // Load previously selected files from SharedPreferences
        val savedFilesJson =
            getSharedPreferences(PREFS_NAME, MODE_PRIVATE).getString(PREFS_SELECTED_FILES, null)

        savedFilesJson?.let {
            val type = object : TypeToken<List<SelectedFile>>() {}.type
            selectedFiles.addAll(Gson().fromJson(it, type))
            fileAdapter.notifyDataSetChanged()
        }

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

                                // Check if the file is not already selected before adding it
                                val selectedFile =
                                    selectedFiles.find { it1 -> it1.uriString == fileUri.toString() }
                                if (selectedFile == null) {
                                    selectedFiles.add(SelectedFile(fileUri.toString(), fileName))
                                    fileAdapter.notifyItemInserted(selectedFiles.size - 1)
                                }
                            }
                        } else {
                            val uri = it.data
                            uri?.let { it1 ->
                                val fileName = getFileName(it1)

                                // Check if the file is not already selected before adding it
                                val selectedFile =
                                    selectedFiles.find { it2 -> it2.uriString == uri.toString() }
                                if (selectedFile == null) {
                                    selectedFiles!!.add(SelectedFile(uri.toString(), fileName))
                                    fileAdapter.notifyItemInserted(selectedFiles.size - 1)
                                }
                            }
                        }
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

    private fun requestOverlayPermission() {
        if (!Settings.canDrawOverlays(this)) {
            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
            intent.data = Uri.parse("package:$packageName")
            requestOverlayPermissionLauncher.launch(intent)
        } else {
            startScreenUnlockService()
        }
    }

    private fun startScreenUnlockService() {
        val serviceIntent = Intent(this, ScreenUnlockForegroundService::class.java)
        serviceIntent.putParcelableArrayListExtra(
            PREFS_SELECTED_FILES, ArrayList(selectedFiles)
        )
        startForegroundService(serviceIntent)
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
