package com.edt.ut3.backend.preferences

import android.content.Context
import androidx.preference.PreferenceManager
import com.edt.ut3.ui.preferences.Theme
import com.edt.ut3.ui.preferences.ThemePreference
import com.elzozor.yoda.utils.DateExtensions.get
import org.json.JSONArray
import org.json.JSONException
import java.util.*

class PreferencesManager(private val context: Context) {

    companion object {
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

    fun getTheme() : Theme {
        val themePreference = ThemePreference.values()
        val selectedTheme = preferences.getString("theme", "0")!!.toInt().coerceIn(0, themePreference.size - 1)
        val theme = ThemePreference.values()[selectedTheme]

        if (theme == ThemePreference.TIME) {
            return getThemeDependingOnTime()
        }


        return Theme.values()[selectedTheme]
    }

    private fun getThemeDependingOnTime(): Theme = PreferencesManager(context).run {
            val start = getDarkStart()
            val end = getDarkEnd()

            val now = Calendar.getInstance().time
            val nowHours = now.get(Calendar.HOUR_OF_DAY)!!
            val nowMinutes = now.get(Calendar.MINUTE)!!

            val startInMinutes = start.toMinutes()
            val minutes = end.toMinutes()
            val endInMinutes = when {
                minutes < startInMinutes -> minutes + (24*60)
                else -> minutes
            }

            val currentInMinutes = nowHours * 60 + nowMinutes

            if (currentInMinutes in startInMinutes until endInMinutes) {
                Theme.DARK
            } else {
                Theme.LIGHT
            }
        }

    private fun getDarkStart() : Time {
        return Time(preferences.getString("dark_theme_start", "00:00")!!)
    }

    private fun getDarkEnd() : Time {
        return Time(preferences.getString("dark_theme_end", "00:00")!!)
    }

    class Time(time: String) {
        var hours: Int
        var minutes: Int

        init {
            if (!time.matches(Regex("\\d{2}:\\d{2}"))) {
                throw IllegalArgumentException("The provided string must match the current pattern :\\d{2}:\\d{2}")
            }

            time.split(":").let {
                hours = it[0].toInt()
                minutes = it[1].toInt()
            }
        }

        override fun toString(): String {
            return "${timeToString(hours)}:${timeToString(minutes)}"
        }

        private fun timeToString(value: Int): String {
            if (value < 10) {
                return "0$value"
            }

            return value.toString()
        }

        fun toMinutes() = hours * 60 + minutes
    }

    /**
     * Return if the user allow notifications
     */
    fun isNotificationEnabled() : Boolean {
        return preferences.getBoolean("notification",true)
    }

}