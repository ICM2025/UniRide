package com.example.uniride

import android.os.Bundle
import android.widget.Toast
import com.example.uniride.databinding.ActivityDriverHomeBinding

class DriverHomeActivity : BottomMenuActivity() {
    private lateinit var binding: ActivityDriverHomeBinding
    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        binding = ActivityDriverHomeBinding.inflate(layoutInflater)

        bottomMenuBinding.container.removeAllViews()
        bottomMenuBinding.container.addView(binding.root)
        // Select specific menu item
        bottomMenuBinding.bottomNav.selectedItemId = R.id.nav_home

        //EXAMPLE
        binding.driverButton.setOnClickListener {
            Toast.makeText(this, "Successful", Toast.LENGTH_SHORT).show()
        }

    }
}