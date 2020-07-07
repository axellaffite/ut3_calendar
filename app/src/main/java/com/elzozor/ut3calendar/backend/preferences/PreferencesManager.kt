package com.elzozor.ut3calendar.backend.preferences

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONException

class PreferencesManager(private val context: Context) {

    companion object {
        const val Preferences_File = "preferences"
        const val Groups = "groups"
    }

    private val preferences = context.getSharedPreferences(Preferences_File, Context.MODE_PRIVATE)

    @Throws(JSONException::class)
    fun getGroups(): JSONArray {
        return JSONArray(preferences.getString(Groups, null))
    }

    fun setGroups(groups: JSONArray) {
        preferences.edit().putString(Groups, groups.toString()).apply()
    }

}