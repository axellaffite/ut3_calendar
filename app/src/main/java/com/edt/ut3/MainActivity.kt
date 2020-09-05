package com.edt.ut3

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.ui.setupWithNavController
import com.edt.ut3.backend.preferences.PreferencesManager
import com.edt.ut3.ui.preferences.Theme
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    companion object {
        var createFunction : (() -> Unit)? = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(null)
        setupTheme()

        setContentView(R.layout.activity_main)

        val navView: BottomNavigationView = findViewById(R.id.nav_view)
        val navController = findNavController(R.id.nav_host_fragment)
        navView.setupWithNavController(navController)

        createFunction?.invoke()
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