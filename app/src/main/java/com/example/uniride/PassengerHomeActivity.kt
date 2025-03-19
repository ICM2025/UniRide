package com.example.uniride

import android.content.Intent
import android.os.Bundle
import com.example.uniride.databinding.ActivityPassengerHomeBinding

class PassengerHomeActivity : BottomMenuActivity() {
    private lateinit var binding: ActivityPassengerHomeBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPassengerHomeBinding.inflate(layoutInflater)

        bottomMenuBinding.container.removeAllViews()
        bottomMenuBinding.container.addView(binding.root)

        bottomMenuBinding.bottomNav.selectedItemId = R.id.nav_home


        binding.btnRouteInProgressPassenger.setOnClickListener {
            val intent = Intent(this, PassengerRouteInProgressActivity::class.java).apply {
                putExtra("ESTIMATED_TIME", 25) // Tiempo estimado inicial
                putExtra("DISTANCE", 3) // Distancia inicial
            }
            startActivity(intent)
        }


    }
}