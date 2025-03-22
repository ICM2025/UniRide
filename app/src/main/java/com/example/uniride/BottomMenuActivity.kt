package com.example.uniride

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.uniride.databinding.ActivityBottomMenuBinding

open class BottomMenuActivity : AppCompatActivity() {

    protected lateinit var bottomMenuBinding: ActivityBottomMenuBinding
    protected var isPassengerMode = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        bottomMenuBinding = ActivityBottomMenuBinding.inflate(layoutInflater)
        setContentView(bottomMenuBinding.root)

        loadCurrentMode()

        // Bottom menu listener
        setupBottomNavigation()
    }

    protected fun setupBottomNavigation() {
        bottomMenuBinding.bottomNav.setOnItemSelectedListener {
            when (it.itemId) {
                R.id.nav_home -> {
                    if (this !is PassengerHomeActivity && isPassengerMode) {
                        startActivity(Intent(this, PassengerHomeActivity::class.java))
                        finish()
                    } else if (this !is DriverHomeActivity && this !is DriverRouteInProgressActivity && !isPassengerMode) {
                        startActivity(Intent(this, DriverHomeActivity::class.java))
                        finish()
                    }
                    true
                }
                R.id.nav_profile -> {
                    if (this !is PassengerProfileActivity && isPassengerMode) {
                        startActivity(Intent(this, PassengerProfileActivity::class.java))
                        finish()
                    } else if (this !is DriverProfileActivity && !isPassengerMode) {
                        startActivity(Intent(this, DriverProfileActivity::class.java))
                        finish()
                    }
                    true
                }
                R.id.nav_settings -> {
                    if (isPassengerMode && this !is PassengerSettingsActivity) {
                        startActivity(Intent(this, PassengerSettingsActivity::class.java))
                        finish()
                    }
                    true
                }
                R.id.nav_manage -> {
                    if (!isPassengerMode && this !is DriverManageTripsActivity) {
                        startActivity(Intent(this, DriverManageTripsActivity::class.java))
                        finish()
                    }
                    true
                }
                R.id.nav_toggle_mode -> {
                    toggleMode()
                    true
                }
                // item selection has NOT been successfully handled
                else -> false
            }
        }
    }

    private fun toggleMode() {
        //Change current mode
        isPassengerMode = !isPassengerMode

        // Save current mode so it is maintained between activities
        //app_preferences: Name of the preferences file
        val sharedPref = getSharedPreferences("app_preferences", Context.MODE_PRIVATE)
        //To allow several operations in an object (edit app_preferences file)
        with(sharedPref.edit()) {
            putBoolean("is_passenger_mode", isPassengerMode)
            apply()
        }

        if (isPassengerMode) {
            loadPassengerMenu()
            startActivity(Intent(this, PassengerHomeActivity::class.java))
        } else {
            loadDriverMenu()
            startActivity(Intent(this, DriverHomeActivity::class.java))
        }
        finish()
    }

    protected fun loadPassengerMenu() {
        bottomMenuBinding.bottomNav.menu.clear()
        bottomMenuBinding.bottomNav.inflateMenu(R.menu.bottom_nav_passenger)
    }

    protected fun loadDriverMenu() {
        bottomMenuBinding.bottomNav.menu.clear()
        bottomMenuBinding.bottomNav.inflateMenu(R.menu.bottom_nav_driver)
    }

    protected fun loadCurrentMode() {
        val sharedPref = getSharedPreferences("app_preferences", Context.MODE_PRIVATE)
        //It begins in passenger mode
        isPassengerMode = sharedPref.getBoolean("is_passenger_mode", true)

        if (isPassengerMode) {
            loadPassengerMenu()
        } else {
            loadDriverMenu()
        }
    }
}