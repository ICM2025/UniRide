package com.example.uniride

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import com.example.uniride.databinding.ActivityDriverRouteInProgressBinding

class DriverRouteInProgressActivity : BottomMenuActivity() {

    private lateinit var binding: ActivityDriverRouteInProgressBinding

    //Values examples
    private var currentTripId: String? = null
    private var availableSeats: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDriverRouteInProgressBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupBottomNavigation()
        loadDriverMenu()

        isPassengerMode = false

        binding.availableSeats.text = availableSeats.toString()

        setupButtonListeners()
    }

    private fun setupButtonListeners() {
        binding.btnMenu.setOnClickListener {
            Toast.makeText(this, "Menú", Toast.LENGTH_SHORT).show()
            finish()
        }

        binding.btnPassenger.setOnClickListener {
            val intent = Intent(this, DriverPassengersListActivity::class.java)
            startActivity(intent)
        }
    }

    // Method to update availables seats
    fun updateAvailableSeats(seats: Int) {
        availableSeats = seats
        binding.availableSeats.text = availableSeats.toString()
    }

    // Method to update next stop info
    fun updateNextStopInfo(distance: Int, address: String) {
        binding.nextStopInfo.text = "Próxima Parada: ${distance}km - ($address)"
    }
}