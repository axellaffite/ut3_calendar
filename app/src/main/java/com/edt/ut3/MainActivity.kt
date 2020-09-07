package com.edt.ut3

import android.os.Bundle
import android.view.View.GONE
import android.view.View.VISIBLE
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.ui.setupWithNavController
import com.edt.ut3.backend.preferences.PreferencesManager
import com.edt.ut3.misc.hideKeyboard
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        PreferencesManager(this).setupTheme()


        val navView: BottomNavigationView = findViewById(R.id.nav_view)
        val controller = findNavController(R.id.nav_host_fragment)

        navView.setupWithNavController(controller)

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
    }

    private fun showBottomNav(bottomNav : BottomNavigationView) {
        bottomNav.visibility = VISIBLE
    }

    private fun hideBottomNav(bottomNav : BottomNavigationView) {
        bottomNav.visibility = GONE
    }
}