package com.example.uniride

import android.content.Intent
import android.os.Bundle
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity

class TarjetaActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tarjeta)

        // Botón de regreso
        val btnBack = findViewById<ImageButton>(R.id.btnBackTarjeta)
        btnBack.setOnClickListener {
            finish() // Regresa a la pantalla anterior
        }

        // Referencias a los 3 botones
        val btnMasterCard = findViewById<ImageButton>(R.id.btnMasterCard)
        val btnVisa = findViewById<ImageButton>(R.id.btnVisa)
        val btnNequi = findViewById<ImageButton>(R.id.btnNequi)

        // Listener para MasterCard
        btnMasterCard.setOnClickListener {
            val intent = Intent(this, InfoTarjetaActivity::class.java)
            startActivity(intent)
        }

        // Listener para Visa
        btnVisa.setOnClickListener {
            val intent = Intent(this, InfoTarjetaActivity::class.java)
            startActivity(intent)
        }

        // Listener para Nequi
        btnNequi.setOnClickListener {
            val intent = Intent(this, InfoTarjetaActivity::class.java)
            startActivity(intent)
        }
    }
}
