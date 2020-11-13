package com.edt.ut3.backend.preferences

import android.content.Context
import android.content.SharedPreferences
import android.content.res.Configuration
import androidx.appcompat.app.AppCompatDelegate.*
import androidx.preference.PreferenceManager
import com.edt.ut3.backend.calendar.CalendarMode
import com.edt.ut3.backend.formation_choice.School
import com.edt.ut3.backend.preferences.simple_preference.SimplePreference
import com.edt.ut3.ui.preferences.Theme
import com.edt.ut3.ui.preferences.ThemePreference

/**
 * Used to manage the application preferences.
 *
 * @property context The application context
 *
 * @param simplePreference The [SimplePreference] used to delegate
 * properties.
 */
class PreferencesManager private constructor(
    private val context: Context,
    simplePreference: SimplePreference
) {

    private val preferences = PreferenceManager.getDefaultSharedPreferences(context)

    companion object {
        private var instance: PreferencesManager? = null

        @Synchronized
        fun getInstance(context: Context) = synchronized(this) {
            if (instance == null) {
                instance = PreferencesManager(context, SimplePreference(context))
            }

            instance!!
        }
    }

    /**
     * Describes the different preferences keys.
     *
     * @property key The key that is used to store
     * the preference.
     */
    enum class PreferenceKeys(val key: String) {
        THEME("theme"),
        LINK("link"),
        GROUPS("groups"),
        CALENDAR_MODE("calendar_mode"),
        NOTIFICATION("notification"),
        FIRST_LAUNCH("first_launch"),
        CODE_VERSION("code_version")
    }


    var theme : ThemePreference by simplePreference.Delegate(
        key = PreferenceKeys.THEME.key,
        defValue = ThemePreference.SMARTPHONE.toString(),
        converter = ThemePreferenceConverter
    )

    var link : School.Info? by simplePreference.Delegate(
        key = PreferenceKeys.LINK.key,
        defValue = null.toString(),
        converter = InfoConverter
    )

    var groups : List<String>? by simplePreference.Delegate(
        key = PreferenceKeys.GROUPS.key,
        defValue = null.toString(),
        converter = StringListConverter
    )

    var calendarMode : CalendarMode by simplePreference.Delegate(
        key = PreferenceKeys.CALENDAR_MODE.key,
        defValue = CalendarMode.default().toJSON().toString(),
        converter = CalendarModeConverter
    )

    var notification : Boolean by simplePreference.Delegate <Boolean>(
        key = PreferenceKeys.NOTIFICATION.key,
        defValue = true.toString(),
        converter = BooleanConverter,
        getter = ::getBooleanFromPreferences
    )

    private var firstLaunch : Boolean by simplePreference.Delegate(
        key = PreferenceKeys.FIRST_LAUNCH.key,
        defValue = true.toString(),
        converter = BooleanConverter,
        getter = ::getBooleanFromPreferences
    )

    var codeVersion : Int by simplePreference.Delegate(
        key = PreferenceKeys.CODE_VERSION.key,
        defValue = 0.toString(),
        converter = IntConverter,
        getter = ::getIntegerFromPreferences
    )

    /**
     * Used to listen to preferences changes.
     *
     * @param observer The preferences observer
     */
    fun observe(observer: SharedPreferences.OnSharedPreferenceChangeListener) = observer.let {
        preferences.registerOnSharedPreferenceChangeListener(it)
    }

    /**
     * Setup the theme depending on the
     * current [theme] set in preferences.
     *
     * By default the theme is configured
     * to follow the smartphone's one.
     */
    fun setupTheme() = when (theme) {
        ThemePreference.SMARTPHONE -> setDefaultNightMode(MODE_NIGHT_FOLLOW_SYSTEM)
        ThemePreference.DARK -> setDefaultNightMode(MODE_NIGHT_YES)
        ThemePreference.LIGHT -> setDefaultNightMode(MODE_NIGHT_NO)
    }

    /**
     * Returned the guessed theme.
     * By default if the [preference][theme]
     * is set to [ThemePreference.SMARTPHONE]
     * and the value is not available, the returned
     * value will be [Theme.LIGHT].
     */
    fun currentTheme() = guessTheme(theme)

    private fun guessTheme(pref: ThemePreference) = when (pref) {
        ThemePreference.DARK -> Theme.DARK
        ThemePreference.LIGHT -> Theme.LIGHT
        ThemePreference.SMARTPHONE -> getThemeDependingOnSmartphone()
    }

    /**
     * Computes the theme depending on the smartphone's one.
     * By default if the smartphone's theme cannot be guessed,
     * the returned value is [Theme.LIGHT].
     *
     * @return The guessed theme or [Theme.LIGHT] by default.
     */
    private fun getThemeDependingOnSmartphone(): Theme {
        val currentNightMode: Int =
            (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK)

        return when (currentNightMode) {
            Configuration.UI_MODE_NIGHT_NO -> Theme.LIGHT
            Configuration.UI_MODE_NIGHT_YES -> Theme.DARK
            else -> Theme.LIGHT
        }
    }

    /**
     * If it is the first launch, initialize the preferences
     * to their default values and set the [firstLaunch]
     * preference to false.
     *
     * @return true if the preferences has been edited (first launch).
     */
    fun setupDefaultPreferences(): Boolean = when (firstLaunch) {
        true -> {
            theme = ThemePreference.SMARTPHONE
            calendarMode = CalendarMode.default()
            notification = true
            firstLaunch = false

            true
        }

        else -> false
    }
}