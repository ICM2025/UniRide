package com.example.uniride

import android.os.Bundle

class PassengerProfileActivity : BottomMenuActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val contentView = layoutInflater.inflate(R.layout.activity_passenger_profile, bottomMenuBinding.container
            , false)
        bottomMenuBinding.container.removeAllViews() // Limpiar el contenedor por si acaso
        bottomMenuBinding.container.addView(contentView)

        bottomMenuBinding.bottomNav.selectedItemId = R.id.nav_profile
    }
}