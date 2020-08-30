package com.edt.ut3.backend.preferences

import android.content.Context
import androidx.preference.PreferenceManager
import org.json.JSONArray
import org.json.JSONException

class PreferencesManager(private val context: Context) {

    companion object {
        const val Preferences_File = "preferences"
        const val Groups = "groups"
    }

    private val preferences = PreferenceManager.getDefaultSharedPreferences(context)

    @Throws(JSONException::class)
    fun getGroups(): JSONArray {
        return JSONArray(preferences.getString(Groups, null))
    }

    fun setGroups(groups: JSONArray) {
        preferences.edit().putString(Groups, groups.toString()).apply()
    }

    fun getTheme() : Int {
        return preferences.getString("theme", null)?.toInt() ?: 0
    }

    /**
     * Return if the user allow notifications
     */
    fun isNotificationEnabled() : Boolean {
        return preferences.getBoolean("notification",true)
    }

}