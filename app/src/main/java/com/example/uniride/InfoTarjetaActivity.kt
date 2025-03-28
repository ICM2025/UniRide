package com.example.uniride

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import com.example.uniride.databinding.ActivityInfoTarjetaBinding

class InfoTarjetaActivity : AppCompatActivity() {
    private lateinit var binding: ActivityInfoTarjetaBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityInfoTarjetaBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Botón de retroceso
        binding.btnBackInfoTarjeta.setOnClickListener {
            finish() // Cierra la pantalla y regresa a TarjetaActivity
        }

        // Botón "Continuar"
        binding.btnContinuar.setOnClickListener {
            // Por ahora no hace nada.
            // Aquí podrías implementar la lógica de validación o siguiente paso.
        }
    }
}
