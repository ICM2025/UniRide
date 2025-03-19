package com.example.uniride

import android.os.Bundle
import com.example.uniride.databinding.ActivityDriverManageTripsBinding

class DriverManageTripsActivity : BottomMenuActivity() {
    private lateinit var binding: ActivityDriverManageTripsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDriverManageTripsBinding.inflate(layoutInflater)

        bottomMenuBinding.container.removeAllViews()
        bottomMenuBinding.container.addView(binding.root)

        bottomMenuBinding.bottomNav.selectedItemId = R.id.nav_manage
    }
}