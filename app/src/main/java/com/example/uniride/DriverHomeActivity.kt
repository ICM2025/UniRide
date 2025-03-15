package com.example.uniride

import android.os.Bundle

class DriverHomeActivity : BottomMenuActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //Charge xml with specific container
        val contentView = layoutInflater.inflate(R.layout.activity_driver_home, bottomMenuBinding.container
            , false)
        bottomMenuBinding.container.removeAllViews()
        bottomMenuBinding.container.addView(contentView)

        // Select specific menu item
        bottomMenuBinding.bottomNav.selectedItemId = R.id.nav_home

    }
}