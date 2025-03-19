package com.example.uniride

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.example.uniride.databinding.ActivityPassengerProfileBinding

class PassengerProfileActivity : BottomMenuActivity() {

    private lateinit var binding: ActivityPassengerProfileBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPassengerProfileBinding.inflate(layoutInflater)
        val rootView = binding.root
        setContentView(rootView)
        super.setupBottomNavigation()

        bottomMenuBinding.bottomNav.selectedItemId = R.id.nav_profile
        loadUserData()

        binding.btnEditProfile.setOnClickListener {
            Toast.makeText(this, "Función de edición próximamente", Toast.LENGTH_SHORT).show()
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

        // Since LoginActivity might not exist yet, we'll handle that gracefully
        try {
            // Attempt to navigate to login screen
            val loginActivityClass = Class.forName("com.example.uniride.LoginActivity")
            val intent = Intent(this, loginActivityClass)
            // Clear back stack
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
        } catch (e: ClassNotFoundException) {
            // If LoginActivity doesn't exist yet, just show a toast
            Toast.makeText(this, "Sesión cerrada correctamente", Toast.LENGTH_SHORT).show()
        }

        finish()
    }
}