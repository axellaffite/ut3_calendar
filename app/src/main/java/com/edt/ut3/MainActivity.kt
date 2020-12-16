package com.edt.ut3

import android.content.SharedPreferences
import android.os.Bundle
import android.view.View.GONE
import android.view.View.VISIBLE
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.ui.setupWithNavController
import com.edt.ut3.backend.firebase_services.FirebaseMessagingHandler
import com.edt.ut3.backend.preferences.PreferencesManager
import com.edt.ut3.compatibility.CompatibilityManager
import com.edt.ut3.misc.extensions.hideKeyboard
import com.edt.ut3.ui.preferences.ThemePreference
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity(), SharedPreferences.OnSharedPreferenceChangeListener {

    private var previousTheme : ThemePreference? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        CompatibilityManager.ensureCompatibility(this)
        CompatibilityManager.to30(this)
        val shouldConfigure: Boolean
        PreferencesManager.getInstance(this).apply {
            shouldConfigure = (link == null || groups.isNullOrEmpty())
            setupDefaultPreferences()
            observe(this@MainActivity)
            setupTheme()
        }


        val navView: BottomNavigationView = findViewById(R.id.nav_view)
        val controller = findNavController(R.id.nav_host_fragment)
        navView.setupWithNavController(controller)

        if (shouldConfigure && controller.currentDestination?.id == R.id.navigation_calendar) {
            controller.navigate(R.id.action_navigation_calendar_to_firstLaunchFragment)
        }

        controller.addOnDestinationChangedListener { _, destination, _ ->
            hideKeyboard()
            when (destination.id) {
                R.id.navigation_notes -> showBottomNav(navView)
                R.id.navigation_calendar -> showBottomNav(navView)
                R.id.navigation_room_finder -> showBottomNav(navView)
                R.id.navigation_map -> showBottomNav(navView)
                else -> hideBottomNav(navView)
            }
        }

        FirebaseMessagingHandler.ensureGroupRegistration(this)
    }

    private fun showBottomNav(bottomNav : BottomNavigationView) {
        bottomNav.visibility = VISIBLE
    }

    private fun hideBottomNav(bottomNav : BottomNavigationView) {
        bottomNav.visibility = GONE
    }

    override fun onSharedPreferenceChanged(preference: SharedPreferences?, key: String?) {
        if (key == "theme") {
            val newTheme = PreferencesManager.getInstance(this).theme
            if (newTheme != previousTheme) {
                previousTheme = newTheme
                PreferencesManager.getInstance(this).setupTheme()
            }
        }
    }

}