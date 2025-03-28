package com.example.uniride

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import com.example.uniride.databinding.ActivityDriverManageTripsBinding

class DriverManageTripsActivity : BottomMenuActivity(){

    private lateinit var binding: ActivityDriverManageTripsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDriverManageTripsBinding.inflate(layoutInflater)
        bottomMenuBinding.container.removeAllViews()
        bottomMenuBinding.container.addView(binding.root)

        bottomMenuBinding.bottomNav.selectedItemId = R.id.nav_manage
        setupUI()
        setupListeners()
    }

    private fun setupUI() {
        // Aquí puedes inicializar la interfaz de usuario
        // Por ejemplo, cargar los viajes programados desde una base de datos

        // Ejemplo de cómo cambiar dinámicamente el estado del viaje actual
        // binding.tvCurrentTripStatus.text = "Viaje en curso: Universidad → Casa"
    }

    private fun setupListeners() {
        binding.btnBack.setOnClickListener {
            val intent = Intent(this, DriverHomeActivity::class.java)
            startActivity(intent)
        }

        binding.btnSettings.setOnClickListener {
            Toast.makeText(this, "Configuración", Toast.LENGTH_SHORT).show()
        }

        binding.btnStartTrip.setOnClickListener {
            startTrip()
        }

        // Configurar el botón de crear nuevo viaje
        binding.btnCreateTrip.setOnClickListener {
            val intent = Intent(this, DriverPublishRouteActivity::class.java)
            startActivity(intent)
        }

        binding.btnEdit.setOnClickListener{
            Toast.makeText(this, "Edicion Viaje Proximamente", Toast.LENGTH_SHORT).show()
        }
    }

    private fun startTrip() {
        binding.tvCurrentTripStatus.text = "Viaje iniciado"
        binding.btnStartTrip.text = "Finalizar Viaje"

        binding.btnStartTrip.setOnClickListener {
            finishTrip()
        }

        Toast.makeText(this, "Viaje iniciado", Toast.LENGTH_SHORT).show()

        val intent = Intent(this, DriverRouteInProgressActivity::class.java)
        startActivity(intent)
    }

    private fun finishTrip() {
        binding.tvCurrentTripStatus.text = "No hay viaje en curso"
        binding.btnStartTrip.text = "Iniciar Viaje"

        // Restaurar la función original del botón
        binding.btnStartTrip.setOnClickListener {
            startTrip()
        }

        Toast.makeText(this, "Viaje finalizado", Toast.LENGTH_SHORT).show()
    }
}