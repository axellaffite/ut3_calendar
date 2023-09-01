package com.edt.ut3

import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.Manifest
import android.util.Log
import android.view.View.GONE
import android.view.View.VISIBLE
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.contract.ActivityResultContracts.RequestPermission
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
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

    private val requestPermissionLauncher =
        registerForActivityResult(RequestPermission()
    ) { isGranted: Boolean ->
            if (isGranted)
                Log.d("Permissions", "Allowed to show notifications")
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        CompatibilityManager().ensureCompatibility(this)
        val shouldConfigure: Boolean
        PreferencesManager.getInstance(this).apply {
            shouldConfigure = (link == null || groups.isNullOrEmpty())
            setupDefaultPreferences()
            observe(this@MainActivity)
            setupTheme()
        }
        Log.d("Permission","Checking if notification permission is necessary : Build version : "+ Build.VERSION.SDK_INT.toString())
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU){
            if(ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED){
                Log.d("Permission","Permission not granted")
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
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