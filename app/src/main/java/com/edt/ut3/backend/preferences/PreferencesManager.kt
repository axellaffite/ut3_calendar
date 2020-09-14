package com.edt.ut3.backend.preferences

import android.content.Context
import android.content.SharedPreferences
import android.content.res.Configuration
import androidx.appcompat.app.AppCompatDelegate.*
import androidx.preference.PreferenceManager
import com.edt.ut3.ui.calendar.CalendarMode
import com.edt.ut3.ui.preferences.Theme
import com.edt.ut3.ui.preferences.ThemePreference
import org.json.JSONArray

class PreferencesManager(private val context: Context) {

    enum class Preference(val value : String) {
        GROUPS("groups"),
        THEME("theme"),
        CALENDAR("calendar_mode"),
        NOTIFICATION("notification")
    }

    private val preferences = PreferenceManager.getDefaultSharedPreferences(context)

    private fun serialize(pref: Preference, value: Any): String {
        val serialized = when (pref) {
            Preference.GROUPS -> with (value as String) { JSONArray(value).toString() }
            Preference.CALENDAR -> with(value as CalendarMode) { toJSON() }
            Preference.THEME -> with (value as ThemePreference) { toString() }
            Preference.NOTIFICATION -> with (value as String) { toString() }
        }

        println("Serializing $value into $serialized")

        return serialized
    }

    private fun deserialize(pref: Preference, value: String?): Any? {
        return when (pref) {
            Preference.GROUPS ->
                value ?.let { JSONArray(value) }

            Preference.CALENDAR ->
                value?.let { CalendarMode.fromJson(it) } ?: CalendarMode.default()

            Preference.THEME -> {
                val selected = value?.let { ThemePreference.valueOf(it) } ?: ThemePreference.SMARTPHONE
                guessTheme(selected)
            }

            Preference.NOTIFICATION ->
                value?.let { it.toBoolean() } ?: true
        }
    }

    fun setPreference(pref: Preference, value: Any) {
        preferences.edit().putString(pref.value, serialize(pref, value)).apply()
    }

    fun get(pref: Preference) =
        deserialize(pref, preferences.getString(pref.value, null))

    fun observe(observer: SharedPreferences.OnSharedPreferenceChangeListener) {
        observer.let {
            preferences.registerOnSharedPreferenceChangeListener(it)
        }
    }

    fun setupTheme() {
        val preference = get(Preference.THEME)

        when (preference) {
            ThemePreference.SMARTPHONE -> setDefaultNightMode(MODE_NIGHT_FOLLOW_SYSTEM)
            ThemePreference.DARK -> setDefaultNightMode(MODE_NIGHT_YES)
            ThemePreference.LIGHT -> setDefaultNightMode(MODE_NIGHT_NO)
        }
    }

    fun guessTheme(pref: ThemePreference) : Theme {
        return when (pref) {
            ThemePreference.DARK -> Theme.DARK
            ThemePreference.LIGHT -> Theme.LIGHT
            ThemePreference.SMARTPHONE -> getThemeDependingOnSmartphone()
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

}