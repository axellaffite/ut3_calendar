package com.edt.ut3.backend.preferences

import android.content.Context
import android.content.SharedPreferences
import android.content.res.Configuration
import android.util.Log
import androidx.appcompat.app.AppCompatDelegate.*
import androidx.preference.PreferenceManager
import com.edt.ut3.backend.calendar.CalendarMode
import com.edt.ut3.backend.formation_choice.School
import com.edt.ut3.backend.preferences.simple_preference.SimplePreference
import com.edt.ut3.misc.extensions.toJSONArray
import com.edt.ut3.misc.extensions.toList
import com.edt.ut3.ui.preferences.Theme
import com.edt.ut3.ui.preferences.ThemePreference
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject


class PreferencesManager private constructor(private val context: Context, simplePreference: SimplePreference) {

    enum class PreferenceKeys(val key: String) {
        THEME("theme"),
        LINK("link"),
        GROUPS("groups"),
        CALENDAR_MODE("calendar_mode"),
        NOTIFICATION("notification"),
        FIRST_LAUNCH("first_launch"),
        CODE_VERSION("code_version")
    }

    abstract class BaseTypeConverter <T>: SimplePreference.Converter<T>() {
        override fun convertToString(value: T) = value.toString()
    }

    object BooleanConverter : BaseTypeConverter<Boolean>() {
        override fun getFromString(value: String) = value.toBoolean()
    }

    object IntConverter : BaseTypeConverter<Int>() {
        override fun getFromString(value: String) = value.toInt()
    }

    private fun getBooleanFromPreferences(pref: SharedPreferences, key: String, def: String): String {
        return try {
            (pref.getString(key, null) ?: def).toString()
        } catch (e: ClassCastException) {
            pref.getBoolean(key, def.toBoolean()).toString()
        }
    }

    private fun getIntegerFromPreferences(pref: SharedPreferences, key: String, def: String): String {
        return try {
            (pref.getString(key, null) ?: def).toString()
        } catch (e: ClassCastException) {
            pref.getInt(key, def.toInt()).toString()
        }
    }

    var theme : ThemePreference by simplePreference.Delegate(
        PreferenceKeys.THEME.key,
        ThemePreference.SMARTPHONE.toString(),
        object: SimplePreference.Converter<ThemePreference>() {
            override fun getFromString(value: String) = ThemePreference.valueOf(value)

            override fun convertToString(value: ThemePreference) = value.toString()
        }
    )

    var link : School.Info? by simplePreference.NullableDelegate(
        PreferenceKeys.LINK.key,
        null,
        object: SimplePreference.NullableConverter<School.Info>() {
            @Throws(JSONException::class)
            override fun getFromString(value: String?) = value?.let {
                School.Info.fromJSON(JSONObject(it))
            }

            override fun convertToString(value: School.Info?) = value?.toJSON()?.toString()
        }
    )

    var groups : List<String>? by simplePreference.NullableDelegate(
        PreferenceKeys.GROUPS.key,
        null,
        object : SimplePreference.NullableConverter<List<String>>() {
            override fun getFromString(value: String?) = value?.let { JSONArray(it).toList<String>() }

            override fun convertToString(value: List<String>?) = value?.toJSONArray { it }?.toString()
        }
    )

    var calendarMode : CalendarMode by simplePreference.Delegate(
        PreferenceKeys.CALENDAR_MODE.key,
        CalendarMode.default().toJSON().toString(),
        object: SimplePreference.Converter<CalendarMode>() {
            @Throws(JSONException::class)
            override fun getFromString(value: String) = CalendarMode.fromJson(value)

            override fun convertToString(value: CalendarMode) = value.toJSON().toString()
        }
    )

    var notification : Boolean by simplePreference.Delegate <Boolean>(
        key = PreferenceKeys.NOTIFICATION.key,
        defValue = "true",
        converter = object : SimplePreference.Converter<Boolean>() {
            override fun getFromString(value: String) = value.toBoolean()

            override fun convertToString(value: Boolean) = value.toString()
        },
        getter = ::getBooleanFromPreferences
    )

    var firstLaunch : Boolean by simplePreference.Delegate(
        key = PreferenceKeys.FIRST_LAUNCH.key,
        defValue = "true",
        converter = BooleanConverter,
        getter = ::getBooleanFromPreferences
    )

    var codeVersion : Int by simplePreference.Delegate(
        PreferenceKeys.CODE_VERSION.key,
        "0",
        IntConverter,
        ::getIntegerFromPreferences
    )

    companion object {
        private var instance: PreferencesManager? = null

        @Synchronized
        fun getInstance(context: Context): PreferencesManager {
            if (instance == null) {
                instance = PreferencesManager(context, SimplePreference(context))
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
        val themePreference = theme
        Log.d(this::class.simpleName, "Setting up $themePreference theme")

        when (themePreference) {
            ThemePreference.SMARTPHONE -> setDefaultNightMode(MODE_NIGHT_FOLLOW_SYSTEM)
            ThemePreference.DARK -> setDefaultNightMode(MODE_NIGHT_YES)
            ThemePreference.LIGHT -> setDefaultNightMode(MODE_NIGHT_NO)
        }
    }

    fun currentTheme() = guessTheme(theme)

    private fun guessTheme(pref: ThemePreference) = when (pref) {
        ThemePreference.DARK -> Theme.DARK
        ThemePreference.LIGHT -> Theme.LIGHT
        ThemePreference.SMARTPHONE -> getThemeDependingOnSmartphone()
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
            theme = ThemePreference.SMARTPHONE
            calendarMode = CalendarMode.default()
            notification = true
            firstLaunch = false

            true
        } else {
            false
        }
    }
}