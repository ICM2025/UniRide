package com.example.uniride

import android.content.Intent
import android.os.Bundle
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import com.example.uniride.databinding.ActivityTarjetaBinding

class TarjetaActivity : AppCompatActivity() {
    private lateinit var binding: ActivityTarjetaBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTarjetaBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Botón de regreso
        binding.btnBackTarjeta.setOnClickListener {
            finish() // Regresa a la pantalla anterior
        }

        // Listener para MasterCard
        binding.btnMasterCard.setOnClickListener {
            val intent = Intent(this, InfoTarjetaActivity::class.java)
            startActivity(intent)
        }

        // Listener para Visa
        binding.btnVisa.setOnClickListener {
            val intent = Intent(this, InfoTarjetaActivity::class.java)
            startActivity(intent)
        }

        // Listener para Nequi
        binding.btnNequi.setOnClickListener {
            val intent = Intent(this, InfoTarjetaActivity::class.java)
            startActivity(intent)
        }
    }
}
