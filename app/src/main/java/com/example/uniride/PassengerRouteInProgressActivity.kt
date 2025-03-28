package com.example.uniride

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import com.example.uniride.databinding.ActivityPassengerRouteInProgressBinding

class PassengerRouteInProgressActivity : BottomMenuActivity() {

    private lateinit var binding: ActivityPassengerRouteInProgressBinding
    private var currentRideId: String? = null
    private var estimatedTime: Int = 25 // In Minutes
    private var distance: Int = 3 // In Km

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPassengerRouteInProgressBinding.inflate(layoutInflater)

        bottomMenuBinding.container.removeAllViews()
        bottomMenuBinding.container.addView(binding.root)

        setupBottomNavigation()
        loadPassengerMenu()

        isPassengerMode = true

        currentRideId = intent.getStringExtra("RIDE_ID")
        estimatedTime = intent.getIntExtra("ESTIMATED_TIME", 25)
        distance = intent.getIntExtra("DISTANCE", 3)

        updateRouteInfo()

        setupButtonListeners()

        bottomMenuBinding.bottomNav.selectedItemId = R.id.nav_home
    }

    private fun updateRouteInfo() {
        binding.estimatedTimeInfo.text = "Tiempo Estimado Llegada: $estimatedTime min"
        binding.distanceInfo.text = "Distancia: $distance Km"
    }

    private fun setupButtonListeners() {
        binding.btnBack.setOnClickListener {
            finish()
        }

        binding.btnMessageDriver.setOnClickListener {
            Toast.makeText(this, "Abrir chat con conductor", Toast.LENGTH_SHORT).show()
            val intent = Intent(this, MessageActivity::class.java)
            startActivity(intent)


            updateEstimatedTime(estimatedTime - 5)
        }
    }

    //Method to update estimated time
    fun updateEstimatedTime(minutes: Int) {
        estimatedTime = minutes
        updateRouteInfo()
    }

    //Method to update distance
    fun updateDistance(kilometers: Int) {
        distance = kilometers
        updateRouteInfo()
    }
}