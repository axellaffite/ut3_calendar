package com.edt.ut3.backend.preferences

import android.content.SharedPreferences
import com.edt.ut3.backend.calendar.CalendarMode
import com.edt.ut3.backend.formation_choice.School
import com.edt.ut3.backend.preferences.simple_preference.SimplePreference
import com.edt.ut3.ui.preferences.ThemePreference
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Used to convert a [Boolean].
 */
object BooleanConverter : SimplePreference.Converter<Boolean, Boolean>() {
    override fun deserialize(value: Boolean): Boolean = value

    override fun serialize(value: Boolean): Boolean = value
}

/**
 * Used to convert a [String].
 */
object StringConverter : SimplePreference.Converter<String, String>() {
    override fun deserialize(value: String): String = value

    override fun serialize(value: String): String = value
}

/**
 * Used to convert an [Int].
 */
object IntConverter : SimplePreference.Converter<Int, Int>() {
    override fun deserialize(value: Int): Int = value

    override fun serialize(value: Int): Int = value
}


/**
 * Used to convert a [ThemePreference]
 */
object ThemePreferenceConverter : SimplePreference.Converter<ThemePreference, String>() {
    override fun deserialize(value: String) = ThemePreference.valueOf(value)
    override fun serialize(value: ThemePreference) = value.toString()
}

/**
 * Used to convert a [School.Info].
 */
object InfoConverter : SimplePreference.Converter<School.Info?, String?>() {
    override fun deserialize(value: String?) = Json.decodeFromString<School.Info?>(value.toString())
    override fun serialize(value: School.Info?) = Json.encodeToString(value)
}

/**
 * Used to convert a list of [String]
 */
object StringListConverter : SimplePreference.Converter<List<String>?, String>() {
    override fun deserialize(value: String) = Json.decodeFromString<List<String>?>(value)
    override fun serialize(value: List<String>?) = Json.encodeToString(value)
}

/**
 * Used to convert a [CalendarMode]
 */
object CalendarModeConverter: SimplePreference.Converter<CalendarMode, String>() {
    override fun deserialize(value: String) = Json.decodeFromString<CalendarMode>(value)
    override fun serialize(value: CalendarMode) = Json.encodeToString(value)
}

fun getBooleanFromPreferences(pref: SharedPreferences, key: String, def: String): String {
    return try {
        (pref.getString(key, null) ?: def).toString()
    } catch (e: ClassCastException) {
        pref.getBoolean(key, def.toBoolean()).toString()
    }
}

fun getIntegerFromPreferences(pref: SharedPreferences, key: String, def: String): String {
    return try {
        (pref.getString(key, null) ?: def).toString()
    } catch (e: ClassCastException) {
        pref.getInt(key, def.toInt()).toString()
    }
}