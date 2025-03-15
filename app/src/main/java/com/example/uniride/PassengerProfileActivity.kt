package com.example.uniride

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.bottomnavigation.BottomNavigationView

class PassengerProfileActivity : BottomMenuActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val contentView = layoutInflater.inflate(R.layout.activity_passenger_profile, binding.container
            , false)
        binding.container.removeAllViews() // Limpiar el contenedor por si acaso
        binding.container.addView(contentView)

        binding.bottomNav.selectedItemId = R.id.nav_profile
    }
}