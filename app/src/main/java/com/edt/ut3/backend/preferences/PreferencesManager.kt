package com.edt.ut3.backend.preferences

import android.content.Context
import android.content.res.Configuration
import androidx.appcompat.app.AppCompatDelegate.*
import androidx.preference.PreferenceManager
import com.edt.ut3.ui.preferences.Theme
import com.edt.ut3.ui.preferences.ThemePreference
import com.elzozor.yoda.utils.DateExtensions.get
import org.json.JSONArray
import org.json.JSONException
import java.util.*

class PreferencesManager(private val context: Context) {

    companion object {
        enum class PreferenceKey (val value: String) {
            GROUPS("groups"), LINK("link")
        }
    }

    private val preferences = PreferenceManager.getDefaultSharedPreferences(context)

    @Throws(JSONException::class)
    fun getGroups(): JSONArray {
        return JSONArray(preferences.getString(PreferenceKey.GROUPS.value, null))
    }

    fun setGroups(groups: List<String>) {
        preferences.edit().putString(PreferenceKey.GROUPS.value, JSONArray(groups).toString()).apply()
    }

    fun setLink(link: String) {
        preferences.edit().putString(PreferenceKey.LINK.value, link).apply()
    }

    fun getLink(): String? {
        return preferences.getString(PreferenceKey.LINK.value, null)
    }

    fun setupTheme(pref: ThemePreference? = null) {
        val preference = pref ?: run {
            val choice = preferences.getString("theme", "0")!!.toInt()
            val possibilities = ThemePreference.values()
            possibilities[choice.coerceAtMost(possibilities.lastIndex)]
        }

        when (preference) {
            ThemePreference.SMARTPHONE -> setDefaultNightMode(MODE_NIGHT_FOLLOW_SYSTEM)
            ThemePreference.DARK -> setDefaultNightMode(MODE_NIGHT_YES)
            ThemePreference.LIGHT -> setDefaultNightMode(MODE_NIGHT_NO)
            ThemePreference.TIME -> getThemeDependingOnTime()
        }
    }

    fun getTheme(pref: ThemePreference? = null) : Theme {
        val themePreference = pref ?: run {
            val choice = preferences.getString("theme", "0")!!.toInt()
            val possibilities = ThemePreference.values()
            possibilities[choice.coerceAtMost(possibilities.lastIndex)]
        }

        return when (themePreference) {
            ThemePreference.DARK -> Theme.DARK
            ThemePreference.LIGHT -> Theme.LIGHT
            ThemePreference.SMARTPHONE -> getThemeDependingOnSmartphone()
            ThemePreference.TIME -> getThemeDependingOnTime()
        }
    }

    private fun getThemeDependingOnSmartphone(): Theme {
        val currentNightMode: Int = (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK)
        return when (currentNightMode) {
            Configuration.UI_MODE_NIGHT_NO -> Theme.LIGHT
            Configuration.UI_MODE_NIGHT_YES -> Theme.DARK
            else -> Theme.LIGHT
        }
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

}