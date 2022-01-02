package com.edt.ut3.refactored.models.repositories.preferences

import android.content.Context
import android.content.SharedPreferences
import android.content.res.Configuration
import androidx.appcompat.app.AppCompatDelegate.*
import androidx.preference.PreferenceManager
import com.edt.ut3.refactored.models.domain.calendar.CalendarMode
import com.edt.ut3.backend.formation_choice.School
import com.edt.ut3.refactored.models.repositories.preferences.simple_preference.SimplePreference
import com.edt.ut3.ui.preferences.Theme
import com.edt.ut3.ui.preferences.ThemePreference
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

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
    sealed class PreferenceKeys<T>(val key: String, val defValue: T) {
        object THEME: PreferenceKeys<ThemePreference>("theme", ThemePreference.SMARTPHONE)
        object LINK: PreferenceKeys<String?>("link", null)
        object GROUPS: PreferenceKeys<List<String>?>("groups", null)
        object OLD_GROUPS: PreferenceKeys<List<String>?>("old_groups", null)
        object CALENDAR_MODE: PreferenceKeys<CalendarMode>("calendar_mode", CalendarMode.default())
        object NOTIFICATION: PreferenceKeys<Boolean>("actual_notification", true)
        object FIRST_LAUNCH: PreferenceKeys<Boolean>("actual_first_launch", true)
        object CODE_VERSION: PreferenceKeys<Int>("actual_code_version", 0)

        object DEPRECATED_FIRST_LAUNCH: PreferenceKeys<Boolean>("first_launch", true)
        object DEPRECATED_NOTIFICATION: PreferenceKeys<Boolean>("notification", true)
    }


    var theme : ThemePreference by simplePreference.Delegate(
        key = PreferenceKeys.THEME.key,
        defValue = PreferenceKeys.THEME.defValue.toString(),
        converter = ThemePreferenceConverter,
        manager = ThemePreferenceManager
    )

    var link : School.Info? by simplePreference.Delegate(
        key = PreferenceKeys.LINK.key,
        defValue = PreferenceKeys.LINK.defValue,
        converter = InfoConverter,
        manager = InfoManager
    )

    var groups : List<String>? by simplePreference.Delegate <List<String>?, String>(
        key = PreferenceKeys.GROUPS.key,
        defValue = PreferenceKeys.GROUPS.defValue.toString(),
        converter = StringListConverter,
        manager = NullableStringListManager
    )

    var oldGroups : List<String>? by simplePreference.Delegate(
        key = PreferenceKeys.OLD_GROUPS.key,
        defValue = PreferenceKeys.OLD_GROUPS.defValue.toString(),
        converter = StringListConverter,
        manager = NullableStringListManager
    )

    var calendarMode : CalendarMode by simplePreference.Delegate(
        key = PreferenceKeys.CALENDAR_MODE.key,
        defValue = Json.encodeToString(PreferenceKeys.CALENDAR_MODE.defValue),
        converter = CalendarModeConverter,
        manager = CalendarModeManager
    )

    var notification : Boolean by simplePreference.Delegate(
        key = PreferenceKeys.NOTIFICATION.key,
        defValue = PreferenceKeys.NOTIFICATION.defValue,
        converter = BooleanConverter,
        manager = BooleanManager
    )

    private var firstLaunch : Boolean by simplePreference.Delegate(
        key = PreferenceKeys.FIRST_LAUNCH.key,
        defValue = PreferenceKeys.FIRST_LAUNCH.defValue,
        converter = BooleanConverter,
        manager = BooleanManager
    )

    var codeVersion : Int by simplePreference.Delegate(
        key = PreferenceKeys.CODE_VERSION.key,
        defValue = PreferenceKeys.CODE_VERSION.defValue,
        converter = IntConverter,
        manager = IntManager
    )

    var deprecated_notification : String by simplePreference.Delegate(
        key = PreferenceKeys.DEPRECATED_NOTIFICATION.key,
        defValue = PreferenceKeys.DEPRECATED_NOTIFICATION.defValue.toString(),
        converter = StringConverter,
        manager = StringManager
    )

    var deprecated_firstLaunch : String by simplePreference.Delegate(
        key = PreferenceKeys.DEPRECATED_FIRST_LAUNCH.key,
        defValue = PreferenceKeys.DEPRECATED_FIRST_LAUNCH.defValue.toString(),
        converter = StringConverter,
        manager = StringManager
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