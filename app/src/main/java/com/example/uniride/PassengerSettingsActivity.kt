package com.example.uniride

import android.content.Intent
import android.os.Bundle
import android.widget.ImageButton

class PassengerSettingsActivity : BottomMenuActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val contentView = layoutInflater.inflate(
            R.layout.activity_passenger_settings,
            bottomMenuBinding.container,
            false
        )
        bottomMenuBinding.container.removeAllViews()
        bottomMenuBinding.container.addView(contentView)

        bottomMenuBinding.bottomNav.selectedItemId = R.id.nav_settings

        // Obtener los botones desde el contentView
        val billeteraBtn = contentView.findViewById<ImageButton>(R.id.billetera)
        val acercaDeBtn = contentView.findViewById<ImageButton>(R.id.acerca_de)

        // Listener para el botón "Billetera"
        billeteraBtn.setOnClickListener {
            val intent = Intent(this, BilleteraActivity::class.java)
            startActivity(intent)
        }

        // Listener para el botón "Acerca de"
        acercaDeBtn.setOnClickListener {
            val intent = Intent(this, AcercaDeActivity::class.java)
            startActivity(intent)
        }
    }
}
