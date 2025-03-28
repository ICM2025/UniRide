package com.example.uniride

import android.content.Intent
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



        binding.btnRouteInProgress.setOnClickListener{
            val intent = Intent(this, DriverRouteInProgressActivity::class.java).apply {
                putExtra("AVAILABLE_SEATS", 0)
            }
            startActivity(intent)
        }

        binding.btnPublishRoute.setOnClickListener{
            val intent = Intent(this, DriverPublishRouteActivity::class.java)
            startActivity(intent)
        }

    }
}