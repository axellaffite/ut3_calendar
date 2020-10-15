package com.edt.ut3.compatibility

import android.content.Context
import android.content.pm.PackageInfo
import com.edt.ut3.backend.formation_choice.School
import com.edt.ut3.backend.preferences.PreferencesManager

object CompatibilityManager {

    val PackageInfo.minorVersion: Int
        get() {
            return if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.P) {
                versionCode
            } else{
                (longVersionCode and 0xFFFF).toInt()
            }
        }

    val Context.packageInfo: PackageInfo
        get() = packageManager.getPackageInfo(packageName, 0)

    private lateinit var preferencesManager: PreferencesManager

    fun ensureCompatibility(context: Context) {
        preferencesManager = PreferencesManager.getInstance(context)

        val oldVersion = preferencesManager.codeVersion
        val newVersion = context.packageInfo.minorVersion

        if (oldVersion == newVersion) {
            return
        }

        when (oldVersion to newVersion) {
            0 to 19 -> from0To19()
        }

        preferencesManager.codeVersion = newVersion
    }

    private fun from0To19() {
        preferencesManager.run {
            if (link?.isNotEmpty() == true) {
                link = School.default.info.first().toJSON().toString()
            } else if (link.isNullOrBlank() && groups?.isNotEmpty() == true) {
                link = School.default.info.first().toJSON().toString()
            }
        }
    }

}