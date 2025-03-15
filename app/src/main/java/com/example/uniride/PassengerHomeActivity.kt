package com.example.uniride

import android.os.Bundle

class PassengerHomeActivity : BottomMenuActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val contentView = layoutInflater.inflate(R.layout.activity_passenger_home, bottomMenuBinding.container
            , false)
        bottomMenuBinding.container.removeAllViews()
        bottomMenuBinding.container.addView(contentView)

        bottomMenuBinding.bottomNav.selectedItemId = R.id.nav_home


    }
}