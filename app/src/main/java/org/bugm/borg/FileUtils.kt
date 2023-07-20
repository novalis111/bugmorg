package org.bugm.borg

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.bugm.borg.AppInitialization.PREFS_NAME
import org.bugm.borg.AppInitialization.PREFS_SELECTED_FILES

fun saveSelectedFilesToSharedPreferences(context: Context, selectedFiles: List<SelectedFile>) {
    val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val editor = prefs.edit()
    val selectedFilesJson = Gson().toJson(selectedFiles)
    editor.putString(PREFS_SELECTED_FILES, selectedFilesJson)
    editor.apply()
}

fun getSelectedFilesFromSharedPreferences(context: Context): MutableList<SelectedFile> {
    val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val selectedFilesJson = prefs.getString(PREFS_SELECTED_FILES, null)
    return Gson().fromJson(
        selectedFilesJson, object : TypeToken<MutableList<SelectedFile>>() {}.type
    ) ?: mutableListOf()
}
