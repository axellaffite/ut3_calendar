package com.edt.ut3.compatibility

import android.content.Context
import android.content.pm.PackageInfo
import android.util.Log
import com.edt.ut3.backend.formation_choice.School
import com.edt.ut3.backend.preferences.PreferencesManager

object CompatibilityManager {

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

    fun ensureCompatibility(context: Context) {
        preferencesManager = PreferencesManager.getInstance(context)

        var oldVersion = preferencesManager.codeVersion
        val newVersion = context.packageInfo.minorVersion

        while (oldVersion < newVersion) {
            oldVersion = migrateFrom(Migration(oldVersion, newVersion))
        }

        updateVersionCode(newVersion)
    }

    private fun updateVersionCode(newVersion: Int) = preferencesManager.run {
        codeVersion = newVersion
    }

    private fun migrateFrom(migration: Migration): Int = when (migration) {
        in Migration(0,19)..Migration(0,29) -> {
            from0To19_29()
            29
        }

        else -> {
            Log.d(
                "CompatibilityManager",
                "Versions are the same or " +
                        "compatibility cannot be done: " +
                        "$migration"
            )

            migration.from + 1
        }
    }

    private fun from0To19_29(): Unit = preferencesManager.run {
        link = School.default.info.first()
    }

    data class Migration(val from: Int, val to: Int): Comparable<Migration> {
        operator fun rangeTo(other: Migration) = MigrationProgression(this, other)

        override fun compareTo(other: Migration): Int {
            return when (val comp = from.compareTo(other.from)) {
                0 -> to.compareTo(other.to)
                else -> comp
            }
        }
    }

    class MigrationIterator(
        val startVersion: Migration,
        val endVersion: Migration): Iterator<Migration> {

        private var currentDate = startVersion

        override fun hasNext() = (currentDate.to < endVersion.to)

        override fun next() = currentDate.copy().also {
            currentDate = currentDate.copy(to = currentDate.to + 1)
        }
    }

    class MigrationProgression(
        override val start: Migration,
        override val endInclusive: Migration
    ) : Iterable<Migration>, ClosedRange<Migration> {

        override fun iterator(): Iterator<Migration> = MigrationIterator(start, endInclusive)

    }

}