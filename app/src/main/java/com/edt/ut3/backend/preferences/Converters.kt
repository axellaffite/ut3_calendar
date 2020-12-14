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
 * The base class which is used to convert
 * basic types such as [Int], [Boolean], [String] etc.
 *
 * @param T The class to serialize / deserialize
 */
abstract class BaseTypeConverter <T>: SimplePreference.Converter<T>() {
    override fun serialize(value: T) = value.toString()
}

/**
 * Used to convert a [Boolean].
 */
object BooleanConverter : BaseTypeConverter<Boolean>() {
    override fun deserialize(value: String) = value.toBoolean()
}

/**
 * Used to convert an [Int].
 */
object IntConverter : BaseTypeConverter<Int>() {
    override fun deserialize(value: String) = value.toInt()
}


/**
 * Used to convert a [ThemePreference]
 */
object ThemePreferenceConverter : SimplePreference.Converter<ThemePreference>() {
    override fun deserialize(value: String) = ThemePreference.valueOf(value)
    override fun serialize(value: ThemePreference) = value.toString()
}

/**
 * Used to convert a [School.Info].
 */
object InfoConverter : SimplePreference.Converter<School.Info?>() {
    override fun deserialize(value: String) = Json.decodeFromString<School.Info?>(value)
    override fun serialize(value: School.Info?) = Json.encodeToString(value)
}

/**
 * Used to convert a list of [String]
 */
object StringListConverter : SimplePreference.Converter<List<String>?>() {
    override fun deserialize(value: String) = Json.decodeFromString<List<String>>(value)
    override fun serialize(value: List<String>?) = Json.encodeToString(value)
}

/**
 * Used to convert a [CalendarMode]
 */
object CalendarModeConverter: SimplePreference.Converter<CalendarMode>() {
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