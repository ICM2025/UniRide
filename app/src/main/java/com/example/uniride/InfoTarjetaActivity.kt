package com.example.uniride

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity

class InfoTarjetaActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_info_tarjeta)

        // Botón de retroceso
        val btnBackInfo = findViewById<ImageButton>(R.id.btnBackInfoTarjeta)
        btnBackInfo.setOnClickListener {
            finish() // Cierra la pantalla y regresa a TarjetaActivity
        }

        // Referencias a los EditText
        val etNumeroTarjeta = findViewById<EditText>(R.id.etNumeroTarjeta)
        val etFechaExp = findViewById<EditText>(R.id.etFechaExp)
        val etCVV = findViewById<EditText>(R.id.etCVV)

        // Botón "Continuar"
        val btnContinuar = findViewById<Button>(R.id.btnContinuar)
        btnContinuar.setOnClickListener {
            // Por ahora no hace nada.
            // Aquí podrías implementar la lógica de validación o siguiente paso.
        }
    }
}
