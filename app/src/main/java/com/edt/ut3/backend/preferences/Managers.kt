package com.edt.ut3.backend.preferences

import android.content.SharedPreferences
import com.edt.ut3.backend.preferences.simple_preference.SimplePreference
import com.edt.ut3.backend.preferences.simple_preference.put


abstract class BaseManager<Converted> : SimplePreference.GetSetManager<Converted>

object BooleanManager : BaseManager<Boolean>() {
    override fun get(key: String, defValue: Boolean, prefs: SharedPreferences): Boolean {
        return prefs.getBoolean(key, defValue)
    }

    override fun set(key: String, value: Boolean, edit: SharedPreferences.Editor) {
        edit.put(key, value)
    }
}

object NullableStringManager : BaseManager<String?>() {
    override fun get(key: String, defValue: String?, prefs: SharedPreferences): String? {
        return prefs.getString(key, defValue) ?: defValue
    }

    override fun set(key: String, value: String?, edit: SharedPreferences.Editor) {
        edit.putString(key, value)
    }
}

object StringManager: BaseManager<String>() {
    override fun get(key: String, defValue: String, prefs: SharedPreferences): String {
        return prefs.getString(key, defValue) ?: defValue
    }

    override fun set(key: String, value: String, edit: SharedPreferences.Editor) {
        edit.putString(key, value)
    }
}

val NullableStringListManager = StringManager

val ThemePreferenceManager = StringManager

val InfoManager = NullableStringManager

val CalendarModeManager = StringManager

object IntManager : BaseManager<Int>() {
    override fun get(key: String, defValue: Int, prefs: SharedPreferences): Int {
        return prefs.getInt(key, defValue)
    }

    override fun set(key: String, value: Int, edit: SharedPreferences.Editor) {
        edit.put(key, value)
    }
}