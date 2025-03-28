package com.example.uniride

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.example.uniride.databinding.ActivityDriverProfileBinding

class DriverProfileActivity : BottomMenuActivity() {
    private lateinit var binding: ActivityDriverProfileBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDriverProfileBinding.inflate(layoutInflater)
        bottomMenuBinding.container.removeAllViews()
        bottomMenuBinding.container.addView(binding.root)
        bottomMenuBinding.bottomNav.selectedItemId = R.id.nav_profile

        binding.btnBack.setOnClickListener {
            startActivity(Intent(this, DriverHomeActivity::class.java))
        }
        loadUserData()

        binding.btnEditProfile.setOnClickListener {
            openEditProfile()
        }

        binding.btnLogout.setOnClickListener {
            confirmLogout()
        }

        binding.btnManageVehicle.setOnClickListener {
            openEditVehicle()
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

    private fun loadUserData() {
        val driverName = "Carlos Rodríguez"
        val driverTrips = "35"
        val driverEmail = "carlos.rod@gmail.com"
        val driverRating = 4.5f

        // Asignar datos a los elementos UI
        binding.Name.text = driverName
        binding.TotalTrips.text = driverTrips
        binding.Email.text = driverEmail
        binding.ratingBar.rating = driverRating
    }

    private fun openEditProfile() {
        val intent = Intent(this, EditProfileActivity::class.java)
        intent.putExtra("IS_DRIVER", true)
        startActivity(intent)
    }

    private fun openEditVehicle() {
        val intent = Intent(this, EditVehicle::class.java)
        // Aquí puedes enviar los datos del vehículo si ya existen
        intent.putExtra("id", "123ABC") // ID de ejemplo
        intent.putExtra("make", "Toyota")
        intent.putExtra("model", "Corolla")
        intent.putExtra("year", 2020)
        intent.putExtra("licensePlate", "XYZ-123")
        intent.putExtra("capacity", 4)
        intent.putExtra("color", "Azul")

        startActivity(intent)
    }
}