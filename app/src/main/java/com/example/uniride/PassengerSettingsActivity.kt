package com.example.uniride

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.bottomnavigation.BottomNavigationView

class PassengerSettingsActivity : BottomMenuActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val contentView = layoutInflater.inflate(R.layout.activity_passenger_settings, binding.container
            , false)
        binding.container.removeAllViews()
        binding.container.addView(contentView)

        binding.bottomNav.selectedItemId = R.id.nav_settings

    }
}