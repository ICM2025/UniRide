package com.example.uniride

import android.content.Intent
import android.os.Bundle
import android.util.Log
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
            if (isPassengerMode) {
                startActivity(Intent(this, PassengerHomeActivity::class.java))
            } else {
                startActivity(Intent(this, DriverHomeActivity::class.java))
            }
        }

        val driverName = intent.getStringExtra("DRIVER_NAME") ?: "Desconocido"
        val driverEmail = intent.getStringExtra("DRIVER_EMAIL") ?: "No disponible"
        val driverImageResId = intent.getIntExtra("DRIVER_IMAGE", -1)

        Log.d("DriverProfileActivity", "Nombre: $driverName, Email: $driverEmail, Imagen: $driverImageResId")

        binding.driverNameText.text = driverName
        binding.driverEmailText.text = driverEmail

        if (driverImageResId != -1) {
            binding.driverImageView.setImageResource(driverImageResId)
        } else {
            binding.driverImageView.setImageResource(R.drawable.ic_profile)
        }

        binding.buttonQualify.setOnClickListener {

            val intent = Intent(this, DriverRateActivity::class.java)
            startActivity(intent)
        }
    }
}

