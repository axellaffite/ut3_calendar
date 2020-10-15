package com.edt.ut3.backend.preferences

import android.content.Context
import android.content.SharedPreferences
import android.content.res.Configuration
import android.util.Log
import androidx.appcompat.app.AppCompatDelegate.*
import androidx.preference.PreferenceManager
import ch.liip.sweetpreferences.SweetPreferences
import com.edt.ut3.backend.calendar.CalendarMode
import com.edt.ut3.ui.preferences.Theme
import com.edt.ut3.ui.preferences.ThemePreference


class PreferencesManager private constructor(private val context: Context, sweetPreferences: SweetPreferences) {

    enum class PreferenceKeys(val key: String) {
        THEME("theme"),
        LINK("link"),
        GROUPS("groups"),
        CALENDAR_MODE("calendar_mode"),
        NOTIFICATION("notification"),
        FIRST_LAUNCH("first_launch"),
        CODE_VERSION("code_version")
    }

    var theme : String by sweetPreferences.delegate(ThemePreference.SMARTPHONE.toString(), PreferenceKeys.THEME.key)
    var link : String? by sweetPreferences.delegate(null, PreferenceKeys.LINK.key)
    var groups : String? by sweetPreferences.delegate(null, PreferenceKeys.GROUPS.key)
    var calendarMode : String by sweetPreferences.delegate(CalendarMode.default().toJSON(), PreferenceKeys.CALENDAR_MODE.key)
    var notification : Boolean by sweetPreferences.delegate(true, PreferenceKeys.NOTIFICATION.key)
    var firstLaunch : Boolean by sweetPreferences.delegate(true, PreferenceKeys.FIRST_LAUNCH.key)
    var codeVersion : Int by sweetPreferences.delegate(0, PreferenceKeys.CODE_VERSION.key)

    companion object {
        private var instance: PreferencesManager? = null

        @Synchronized
        fun getInstance(context: Context): PreferencesManager {
            if (instance == null) {
                val sweetPreferences = SweetPreferences.Builder().withDefaultSharedPreferences(context).build()

                instance = PreferencesManager(context, sweetPreferences)
            }

            return instance!!
        }
    }

    private val preferences = PreferenceManager.getDefaultSharedPreferences(context)

    fun observe(observer: SharedPreferences.OnSharedPreferenceChangeListener) {
        observer.let {
            preferences.registerOnSharedPreferenceChangeListener(it)
        }
    }

    fun setupTheme() {
        val themePreference = ThemePreference.valueOf(theme)
        Log.d(this::class.simpleName, "Setting up $themePreference theme")

        when (themePreference) {
            ThemePreference.SMARTPHONE -> setDefaultNightMode(MODE_NIGHT_FOLLOW_SYSTEM)
            ThemePreference.DARK -> setDefaultNightMode(MODE_NIGHT_YES)
            ThemePreference.LIGHT -> setDefaultNightMode(MODE_NIGHT_NO)
        }
    }

    fun currentTheme() : Theme {
        val themePreference = ThemePreference.valueOf(theme)
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

    @Suppress("IMPLICIT_CAST_TO_ANY")
    fun setupDefaultPreferences(): Boolean {
        return if (firstLaunch) {
            theme = ThemePreference.SMARTPHONE.toString()
            calendarMode = CalendarMode.default().toJSON()
            notification = true
            firstLaunch = false

            true
        } else {
            false
        }
    }
}