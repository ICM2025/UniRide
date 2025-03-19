package com.example.uniride

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.uniride.databinding.ActivityDriverAviableBinding
import com.example.uniride.databinding.ActivityDriverDetailsBinding

class DriverDetailsActivity : AppCompatActivity() {
    private lateinit var binding : ActivityDriverDetailsBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_driver_details)
        binding = ActivityDriverDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val driverName = intent.getStringExtra("DRIVER_NAME") ?: "Desconocido"
        val driverEmail = intent.getStringExtra("DRIVER_EMAIL") ?: "No disponible"
        val driverImageResId = intent.getIntExtra("DRIVER_IMAGE", -1)

        Log.d("DriverDetailActivity", "Nombre: $driverName, Email: $driverEmail, Imagen: $driverImageResId")

        findViewById<TextView>(R.id.driverNameText).text = driverName
        findViewById<TextView>(R.id.driverEmailText).text = driverEmail

        val driverImageView = findViewById<ImageView>(R.id.driverImageView)
        if (driverImageResId != -1) {
            driverImageView.setImageResource(driverImageResId)
        } else {
            driverImageView.setImageResource(R.drawable.ic_profile)  // Imagen de respaldo
        }

        binding.buttonQualify.setOnClickListener() {
            val intent = Intent(this, DriverRateActivity::class.java)
            startActivity(intent)
        }


    }
}