package com.example.uniride

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity

class BilleteraActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_billetera)

        // Botón de regreso
        val btnBack = findViewById<ImageButton>(R.id.btnBackBilletera)
        btnBack.setOnClickListener {
            finish()  // Regresa a la actividad anterior
        }

        // Botón Depositar - Tarjeta
        val btnDepositar = findViewById<Button>(R.id.btnDepositar)
        btnDepositar.setOnClickListener {
            val intent = Intent(this, TarjetaActivity::class.java)
            startActivity(intent)
        }

        // Botón Salir
        val btnSalir = findViewById<Button>(R.id.btnSalir)
        btnSalir.setOnClickListener {
            finish() // Cierra la actividad
        }
    }
}
