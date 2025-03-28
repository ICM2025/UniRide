package com.example.uniride

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import com.example.uniride.databinding.ActivityPassengerProfileBinding

class PassengerProfileActivity : BottomMenuActivity() {

    private lateinit var binding: ActivityPassengerProfileBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPassengerProfileBinding.inflate(layoutInflater)
        bottomMenuBinding.container.removeAllViews()
        bottomMenuBinding.container.addView(binding.root)
        bottomMenuBinding.bottomNav.selectedItemId = R.id.nav_profile

        binding.btnBack.setOnClickListener {
            if (isPassengerMode) {
                startActivity(Intent(this, PassengerHomeActivity::class.java))
            } else {
                startActivity(Intent(this, DriverHomeActivity::class.java))
            }
        }
        loadUserData()

        binding.btnEditProfile.setOnClickListener {
            openEditProfile()
        }

        binding.btnLogout.setOnClickListener {
            confirmLogout()
        }
    }

    private fun loadUserData() {
        val sharedPref = getSharedPreferences("user_data", Context.MODE_PRIVATE)
        val tripsPref = getSharedPreferences("trips_data", Context.MODE_PRIVATE)

        binding.Name.text = sharedPref.getString("name", "Nombre no disponible")
        binding.Email.text = sharedPref.getString("email", "Email no disponible")

        val totalTrips = tripsPref.getInt("passenger_total_trips", 0)
        binding.TotalTrips.text = totalTrips.toString()

        if (sharedPref.getString("name", "").isNullOrEmpty()) {
            binding.Name.text = "Juan Esteban"
            binding.Email.text = "jban@gmail.com"
            binding.TotalTrips.text = "10"
        }
    }

    private fun confirmLogout() {
        AlertDialog.Builder(this)
            .setTitle("Cerrar sesión")
            .setMessage("¿Estás seguro que deseas cerrar la sesión?")
            .setPositiveButton("Sí") { _, _ ->
                logout()
            }
            .setNegativeButton("No", null)
            .show()
    }

    private fun logout() {
        val sharedPref = getSharedPreferences("app_preferences", Context.MODE_PRIVATE)
        with(sharedPref.edit()) {
            putBoolean("is_logged_in", false)
            apply()
        }

        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)

        finish()
    }

    private fun openEditProfile() {
        val intent = Intent(this, EditProfileActivity::class.java)
        intent.putExtra("IS_DRIVER", false)
        startActivity(intent)
    }
}