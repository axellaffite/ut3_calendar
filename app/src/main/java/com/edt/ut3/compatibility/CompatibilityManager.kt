package com.edt.ut3.compatibility

import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageInfo
import android.util.Log
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import com.edt.ut3.backend.formation_choice.School
import com.edt.ut3.backend.preferences.PreferencesManager
import kotlinx.serialization.json.Json

class CompatibilityManager {

    private val PackageInfo.minorVersion: Int
        get() {
            return if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.P) {
                versionCode
            } else{
                (longVersionCode and 0xFFFF).toInt()
            }
        }

    private val Context.packageInfo: PackageInfo
        get() = packageManager.getPackageInfo(packageName, 0)

    private lateinit var preferencesManager: PreferencesManager
    private lateinit var androidPreferencesManager : SharedPreferences

    fun ensureCompatibility(context: Context) {
        preferencesManager = PreferencesManager.getInstance(context)
        androidPreferencesManager = PreferenceManager.getDefaultSharedPreferences(context)

        var oldVersion : Int = androidPreferencesManager.run {
            try {
                return@run getString(
                        PreferencesManager.PreferenceKeys.CODE_VERSION.key,
                        PreferencesManager.PreferenceKeys.CODE_VERSION.defValue.toString()
                )?.toInt() ?: PreferencesManager.PreferenceKeys.CODE_VERSION.defValue
            } catch (e: Exception) {
                try {
                    return@run getInt(
                            PreferencesManager.PreferenceKeys.CODE_VERSION.key,
                            PreferencesManager.PreferenceKeys.CODE_VERSION.defValue
                    )
                } catch (e: Exception) {
                    return@run 0
                }
            }
        }

        val newVersion = context.packageInfo.minorVersion

        while (oldVersion < newVersion) {
            val upgrade = migrateFrom(oldVersion, context)
            Log.d("COMPATIBILITY MANAGER", "UPGRADE FROM $oldVersion TO $upgrade DONE !")
            oldVersion = upgrade
        }

        Log.d("COMPATIBILITY MANAGER", "Upgrade to the last version done !")

        updateVersionCode(newVersion)
    }

    private fun updateVersionCode(newVersion: Int) = preferencesManager.run {
        codeVersion = newVersion
    }

    private fun migrateFrom(version: Int, context: Context): Int = when (version) {
        in 0 .. 29 -> to30(context)
        in 30 .. 32 -> to33(context)
        in 33 .. 42 -> to44(context)

        else -> {
            Log.d(
                "CompatibilityManager",
                "Versions are the same or " +
                        "compatibility cannot be done: " +
                        "$version"
            )

            version + 1
        }
    }

    private fun to30(context: Context): Int = preferencesManager.run {
        val androidPreferencesManager = PreferenceManager.getDefaultSharedPreferences(context)
        androidPreferencesManager.run {
            edit {
                try {
                    val notification = PreferencesManager.PreferenceKeys.NOTIFICATION
                    val notificationValue = preferencesManager.deprecated_notification.toBoolean()
                    putBoolean(
                            notification.key,
                            notificationValue
                    )
                } catch (e: Exception) {

                }

                try {
                    val firstLaunch = PreferencesManager.PreferenceKeys.FIRST_LAUNCH
                    val firstLaunchValue = preferencesManager.deprecated_firstLaunch.toBoolean()
                    putBoolean(
                            firstLaunch.key,
                            firstLaunchValue
                    )
                } catch (e: Exception) {

                }

                try {
                    val codeVersion = PreferencesManager.PreferenceKeys.CODE_VERSION
                    putInt(
                            PreferencesManager.PreferenceKeys.NOTIFICATION.key,
                            (getString(codeVersion.key, null)
                                    ?: codeVersion.defValue.toString()).toInt()
                    )
                } catch (e: Exception) {

                }
            }
        }

        30
    }

    private fun to33(context: Context): Int {
        return 33
    }

    private fun to44(context: Context): Int {
        PreferencesManager.getInstance(context).school = context.assets
            .open("schools.json")
            .use { it.bufferedReader().readText() }
            .let { schoolsJson -> Json.decodeFromString<Array<School.Info>>(schoolsJson) }
            .first { school -> school.label == "ut3_fsi" }

        return 43
    }

}