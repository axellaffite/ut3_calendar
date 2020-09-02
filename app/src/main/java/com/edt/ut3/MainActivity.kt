package com.edt.ut3

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupWithNavController
import com.edt.ut3.backend.preferences.PreferencesManager
import com.edt.ut3.ui.preferences.Theme
import com.google.android.material.bottomnavigation.BottomNavigationView
import java.lang.RuntimeException
import kotlin.time.ExperimentalTime

class MainActivity : AppCompatActivity() {

    @ExperimentalTime
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupTheme()

        setContentView(R.layout.activity_main)

        val navView: BottomNavigationView = findViewById(R.id.nav_view)

        val navController = findNavController(R.id.nav_host_fragment)
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        val appBarConfiguration = AppBarConfiguration(setOf(
                R.id.navigation_calendar, R.id.navigation_notes, R.id.navigation_map))
//        setupActionBarWithNavController(navController, appBarConfiguration)

        navView.setupWithNavController(navController)
    }

    private fun setupTheme() {
        val theme = PreferencesManager(this).getTheme()
        when (theme) {
            Theme.LIGHT -> setLightTheme()
            Theme.DARK -> setDarkTheme()
        }
    }

    private fun setLightTheme() {
        setTheme(R.style.AppTheme)
    }

    private fun setDarkTheme() {
        setTheme(R.style.DarkTheme)
    }
}