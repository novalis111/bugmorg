package org.bugm.borg

import android.app.Activity
import android.Manifest
import android.content.ContentValues.TAG
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Parcel
import android.os.Parcelable
import android.provider.OpenableColumns
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.bugm.borg.AppInitialization.PREFS_NAME
import org.bugm.borg.AppInitialization.PREFS_SELECTED_FILES
import org.bugm.borg.AppInitialization.PREFS_TASK_ITEMS
import org.bugm.borg.AppInitialization.PREFS_TASK_ITEMS_TIMESTAMP
import pub.devrel.easypermissions.AppSettingsDialog
import pub.devrel.easypermissions.EasyPermissions
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

    companion object {
        const val RC_PERMISSIONS = 123
    }

    private val permissions = arrayOf(
        Manifest.permission.POST_NOTIFICATIONS,
        Manifest.permission.FOREGROUND_SERVICE,
        Manifest.permission.RECEIVE_BOOT_COMPLETED,
        Manifest.permission.INTERNET,
        Manifest.permission.SYSTEM_ALERT_WINDOW,
        Manifest.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
        Manifest.permission.MANAGE_MEDIA,
        Manifest.permission.MANAGE_EXTERNAL_STORAGE,
        Manifest.permission.MANAGE_DOCUMENTS
    )

    private val requestReadPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val data: Intent? = result.data
            data?.data?.let { uri ->
                val fileName = getFileName(uri)
                if (!fileAlreadyExists(fileName)) {
                    // Grant permanent permission to access this file
                    grantPersistableUriPermission(uri)
                    // Store in Prefs
                    selectedFiles.add(SelectedFile(uri.toString(), fileName))
                    fileAdapter.notifyDataSetChanged()
                    saveSelectedFilesToSharedPreferences(this@MainActivity, selectedFiles)
                    // Update the service with the latest selected files when a file is added
                    ScreenUnlockForegroundService.startService(this@MainActivity)
                }
            }
        } else {
            // Permission denied or user canceled the permission request, handle the case appropriately
        }
    }

    private fun grantPersistableUriPermission(uri: Uri) {
        val takeFlags: Int =
            (Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        contentResolver.takePersistableUriPermission(uri, takeFlags)
    }

    private val requestOverlayPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val foo = "bar"
        } else {
            // Permission denied, handle accordingly (show a message, etc.)
        }
    }

    private fun openFilePicker() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        intent.type = "text/*"
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        requestReadPermissionLauncher.launch(intent)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Check and request the SYSTEM_ALERT_WINDOW permission on devices with Android 12 (API 31) and above
        requestOverlayPermission()
        requestPermissionsIfNeeded()

        // Init file adapter and recycler view for selected files
        selectedFiles = getSelectedFilesFromSharedPreferences(this)
        fileAdapter = FileAdapter(selectedFiles)
        val recyclerView = findViewById<RecyclerView>(R.id.fileRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = fileAdapter

        findViewById<View>(R.id.selectFilesButton).setOnClickListener {
            openFilePicker()
        }

        val btnCleanCache = findViewById<Button>(R.id.clearCacheButton)

        // Set a click listener for the button
        btnCleanCache.setOnClickListener {
            // Call the cleanCache method to remove the cached task items
            clearCache()
        }
    }

    private fun clearCache() {
        val prefs = getSharedPreferences(PREFS_TASK_ITEMS, Context.MODE_PRIVATE)
        prefs.edit().remove(PREFS_TASK_ITEMS).remove(PREFS_TASK_ITEMS_TIMESTAMP).apply()
        Toast.makeText(this, "Cache cleaned successfully", Toast.LENGTH_SHORT).show()
    }

    private fun fileAlreadyExists(fileName: String): Boolean {
        for (file in selectedFiles) {
            if (file.name == fileName) {
                return true
            }
        }
        return false
    }

    private fun requestPermissionsIfNeeded() {
        if (EasyPermissions.hasPermissions(this, *permissions)) {
            // All permissions are already granted, proceed with your app logic
            // ...
        } else {
            // Request permissions
            EasyPermissions.requestPermissions(
                this,
                "Your app needs these permissions for functionality.",
                RC_PERMISSIONS,
                *permissions
            )
        }
    }

    private fun onPermissionsDenied() {
        // Handle the case when the user denied a permission
        // You may show a message explaining why you need the permission and guide the user to grant it.
        // Optionally, you can also show an AppSettingsDialog to allow the user to navigate to app settings and grant the permission.
        if (EasyPermissions.somePermissionPermanentlyDenied(this, permissions.asList())) {
            AppSettingsDialog.Builder(this).build().show()
        } else {
            // Show a message or take appropriate action for the denied permission
            Toast.makeText(this, "Some permissions were denied.", Toast.LENGTH_SHORT).show()
        }
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
