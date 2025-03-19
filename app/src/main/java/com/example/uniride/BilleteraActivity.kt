package com.example.uniride

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import com.example.uniride.databinding.ActivityBilleteraBinding

class BilleteraActivity : AppCompatActivity() {
    private lateinit var binding: ActivityBilleteraBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBilleteraBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Botón de regreso
        binding.btnBackBilletera.setOnClickListener {
            finish()  // Regresa a la actividad anterior
        }

        // Botón Depositar - Tarjeta
        binding.btnDepositar.setOnClickListener {
            val intent = Intent(this, TarjetaActivity::class.java)
            startActivity(intent)
        }

        // Botón Salir
        binding.btnSalir.setOnClickListener {
            finish() // Cierra la actividad
        }
    }
}
