package com.edt.ut3.refactored.models.repositories.preferences.simple_preference

import android.content.SharedPreferences

fun SharedPreferences.Editor.put(key: String, value: String) {
    putString(key, value)
}

fun SharedPreferences.Editor.put(key: String, value: Boolean) {
    putBoolean(key, value)
}

fun SharedPreferences.Editor.put(key: String, value: Int) {
    putInt(key, value)
}

fun SharedPreferences.Editor.put(key: String, value: Long) {
    putLong(key, value)
}

fun SharedPreferences.Editor.put(key: String, value: Float) {
    putFloat(key, value)
}

fun SharedPreferences.Editor.put(key: String, values: Set<String>) {
    putStringSet(key, values)
}