package com.edt.ut3.backend.preferences.simple_preference

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import kotlin.reflect.KProperty

class SimplePreference(context: Context) {

    private val preferences = PreferenceManager.getDefaultSharedPreferences(context)

    inner class Delegate <T> (
        private val key: String,
        private val defValue: String,
        private val converter: Converter<T>,
        private val getter: (pref: SharedPreferences, key: String, defValue: String) -> String =
            { pref, key, def -> pref.getString(key, null) ?: def }
    ) {
        operator fun getValue(thisRef: Any?, property: KProperty<*>): T {
            val value = getter(preferences, key, defValue)
            return converter.getFromString(value)
        }

        operator fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
            preferences.edit {
                putString(key, converter.convertToString(value))
            }
        }
    }

    inner class NullableDelegate <T> (
        private val key: String,
        private val defValue: String?,
        private val converter: NullableConverter<T>
    ) {
        operator fun getValue(thisRef: Any?, property: KProperty<*>): T? {
            return converter.getFromString(preferences.getString(key, defValue))
        }

        operator fun setValue(thisRef: Any?, property: KProperty<*>, value: T?) {
            preferences.edit {
                putString(key, converter.convertToString(value))
            }
        }
    }

    abstract class NullableConverter <T> {
        abstract fun getFromString(value: String?): T?
        abstract fun convertToString(value: T?): String?
    }

    abstract class Converter <T> {
        abstract fun getFromString(value: String): T
        abstract fun convertToString(value: T): String
    }

}