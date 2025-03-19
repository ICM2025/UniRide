package com.example.uniride

import android.os.Bundle
import com.example.uniride.databinding.ActivityDriverProfileBinding

class DriverProfileActivity : BottomMenuActivity() {
    private lateinit var binding: ActivityDriverProfileBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDriverProfileBinding.inflate(layoutInflater)

        bottomMenuBinding.container.removeAllViews()
        bottomMenuBinding.container.addView(binding.root)

        bottomMenuBinding.bottomNav.selectedItemId = R.id.nav_profile
    }
}