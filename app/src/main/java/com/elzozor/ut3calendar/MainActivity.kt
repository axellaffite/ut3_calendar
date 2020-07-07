package com.elzozor.ut3calendar

import android.os.Bundle
import android.util.Log
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.elzozor.ut3calendar.backend.requests.RequestsManager
import com.elzozor.ut3calendar.backend.requests.Utils.generateCelcatBody
import com.elzozor.ut3calendar.misc.minus
import okhttp3.RequestBody
import java.util.*
import kotlin.time.ExperimentalTime
import kotlin.time.days

class MainActivity : AppCompatActivity() {

    @ExperimentalTime
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val navView: BottomNavigationView = findViewById(R.id.nav_view)

        RequestsManager(this).celcatService().let {
            lifecycleScope.launchWhenCreated {
                val body = generateCelcatBody(
                    Date() - (6.days * 31),
                    Date(),
                    listOf("LINF6TDA1")
                )

                Log.d("test", body)

                val events = it.getEvents(body)

                println(events)
            }
        }

        val navController = findNavController(R.id.nav_host_fragment)
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        val appBarConfiguration = AppBarConfiguration(setOf(
                R.id.navigation_calendar, R.id.navigation_notes, R.id.navigation_notifications))
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)
    }
}