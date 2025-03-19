package com.example.uniride

import android.content.Intent
import android.os.Bundle
import android.widget.ImageButton
import com.example.uniride.databinding.ActivityPassengerSettingsBinding

class PassengerSettingsActivity : BottomMenuActivity() {
    private lateinit var binding: ActivityPassengerSettingsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPassengerSettingsBinding.inflate(layoutInflater)

        bottomMenuBinding.container.removeAllViews()
        bottomMenuBinding.container.addView(binding.root)

        bottomMenuBinding.bottomNav.selectedItemId = R.id.nav_settings

        // Listener para el botón "Billetera"
        binding.billetera.setOnClickListener {
            val intent = Intent(this, BilleteraActivity::class.java)
            startActivity(intent)
        }

        // Listener para el botón "Acerca de"
        binding.acercaDe.setOnClickListener {
            val intent = Intent(this, AcercaDeActivity::class.java)
            startActivity(intent)
        }
    }
}
