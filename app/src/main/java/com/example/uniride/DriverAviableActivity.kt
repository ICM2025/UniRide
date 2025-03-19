package com.example.uniride

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.uniride.databinding.ActivityDriverAviableBinding
import com.example.uniride.model.Driver

class DriverAviableActivity : AppCompatActivity() {
    private lateinit var binding: ActivityDriverAviableBinding
    private val drivers = listOf(
        Driver("Andres Castro","Castro@gmail.com", R.drawable.ic_profile),
        Driver("Maria Herrera","Herrerita@gmail.com", R.drawable.ic_profile)
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityDriverAviableBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val adapter = DriverAdapter(this, drivers)
        binding.driverData.adapter = adapter

        binding.driverData.setOnItemClickListener { _, _, position, _ ->
            val selectedDriver = drivers[position]

            val intent = Intent(this, DriverProfileActivity::class.java)
            intent.putExtra("DRIVER_NAME", selectedDriver.name)
            intent.putExtra("DRIVER_IMAGE", selectedDriver.imageResId)
            intent.putExtra("DRIVER_EMAIL", selectedDriver.email)
            startActivity(intent)
        }
    }
}
