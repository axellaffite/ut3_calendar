package com.edt.ut3.backend.preferences

import android.content.Context
import android.content.SharedPreferences
import android.content.res.Configuration
import android.util.Log
import androidx.appcompat.app.AppCompatDelegate.*
import androidx.preference.PreferenceManager
import com.edt.ut3.backend.calendar.CalendarMode
import com.edt.ut3.ui.preferences.Theme
import com.edt.ut3.ui.preferences.ThemePreference
import org.json.JSONArray
import java.io.IOException

class PreferencesManager(private val context: Context) {

    enum class Preference(val value : String) {
        GROUPS("groups"),
        LINK("link"),
        THEME("theme"),
        CALENDAR("calendar_mode"),
        NOTIFICATION("notification"),
    }

    private val preferences = PreferenceManager.getDefaultSharedPreferences(context)

    @Throws(PreferenceCastException::class)
    private fun serialize(pref: Preference, value: Any): String {
        val serialized = try {
            when (pref) {
                Preference.GROUPS ->
                    with (value as List<*>) { JSONArray(this).toString() }

                Preference.CALENDAR ->
                    with(value as CalendarMode) { toJSON() }

                Preference.THEME ->
                    with (value as ThemePreference) { toString() }

                Preference.NOTIFICATION ->
                    with (value as Boolean) { toString() }

                Preference.LINK ->
                    with (value as String) { this }
            }
        } catch (e: Exception) {
            throw PreferenceCastException("Unable to serialize $value to $pref preference !")
        }

        Log.d(this::class.simpleName, "Serializing $value into $serialized")

        return serialized
    }

    @Suppress("UNCHECKED_CAST")
    @Throws(PreferenceCastException::class)
    private fun deserialize(pref: Preference, value: String?): Any? {
        return try {
            when (pref) {
                Preference.GROUPS ->
                    value ?.let { JSONArray(value) }

                Preference.CALENDAR ->
                    value?.let { CalendarMode.fromJson(it) } ?: CalendarMode.default()

                Preference.THEME -> {
                    value?.let { ThemePreference.valueOf(it) } ?: ThemePreference.SMARTPHONE
                }

                Preference.NOTIFICATION ->
                    value?.let { it.toBoolean() } ?: true

                Preference.LINK -> value
            }
        } catch (e: Exception) {
            throw PreferenceCastException("Unable to deserialize $value to $pref preference !")
        }
    }


    @Throws(PreferenceCastException::class)
    fun set(pref: Preference, value: Any) {
        preferences.edit().putString(pref.value, serialize(pref, value)).apply()
    }

    @Suppress("UNCHECKED_CAST")
    fun<T> get(pref: Preference, default: T): T {
        return try {
            deserialize(pref, preferences.getString(pref.value, null)) as T
        } catch (e: Exception) {
            default
        }
    }

    fun observe(observer: SharedPreferences.OnSharedPreferenceChangeListener) {
        observer.let {
            preferences.registerOnSharedPreferenceChangeListener(it)
        }
    }

    fun setupTheme() {
        val preference = get(Preference.THEME, ThemePreference.SMARTPHONE)

        Log.d(this::class.simpleName, "Setting up $preference theme")

        when (preference) {
            ThemePreference.SMARTPHONE -> setDefaultNightMode(MODE_NIGHT_FOLLOW_SYSTEM)
            ThemePreference.DARK -> setDefaultNightMode(MODE_NIGHT_YES)
            ThemePreference.LIGHT -> setDefaultNightMode(MODE_NIGHT_NO)
        }
    }

    fun currentTheme() : Theme {
        val themePreference = get(Preference.THEME, ThemePreference.SMARTPHONE)
        return guessTheme(themePreference)
    }

    private fun guessTheme(pref: ThemePreference) : Theme {
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


    class PreferenceCastException(reason: String): IOException(reason)

}