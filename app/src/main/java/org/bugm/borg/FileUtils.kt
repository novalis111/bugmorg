package org.bugm.borg

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.bugm.borg.AppInitialization.PREFS_NAME
import org.bugm.borg.AppInitialization.PREFS_SELECTED_FILES

fun saveSelectedFilesToSharedPreferences(context: Context, selectedFiles: List<SelectedFile>) {
    val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val selectedFilesJson = Gson().toJson(selectedFiles)
    val editor = prefs.edit()
    editor.putString(PREFS_SELECTED_FILES, selectedFilesJson)
    editor.apply()
}

fun getSelectedFilesFromSharedPreferences(context: Context): MutableList<SelectedFile> {
    val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val selectedFilesJson = prefs.getString(PREFS_SELECTED_FILES, null)
    return if (!selectedFilesJson.isNullOrEmpty()) {
        val listType = object : TypeToken<MutableList<SelectedFile>>() {}.type
        Gson().fromJson(selectedFilesJson, listType)
    } else {
        mutableListOf()
    }
}
