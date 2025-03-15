package com.example.uniride

import android.os.Bundle

class DriverProfileActivity : BottomMenuActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val contentView = layoutInflater.inflate(R.layout.activity_driver_profile, bottomMenuBinding.container, false)
        bottomMenuBinding.container.removeAllViews()
        bottomMenuBinding.container.addView(contentView)

        bottomMenuBinding.bottomNav.selectedItemId = R.id.nav_profile

    }
}