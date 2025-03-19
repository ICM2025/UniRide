package com.example.uniride

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.uniride.databinding.ActivityMessageBinding

class MessageActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMessageBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMessageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.txtNombreUsuario.text = "Usuario"

        setupListeners()
    }

    private fun setupListeners() {
        binding.btnBack.setOnClickListener {
            finish()
        }

        binding.btnAttachFile.setOnClickListener {
            Toast.makeText(this, "Funcionalidad pendiente para implementar", Toast.LENGTH_SHORT).show()
        }

        binding.btnSend.setOnClickListener {
            val mensaje = binding.editMensaje.text.toString().trim()
            if (mensaje.isEmpty()) {
                Toast.makeText(this, "Por favor escribe un mensaje", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            Toast.makeText(this, "Mensaje enviado (simulado)", Toast.LENGTH_SHORT).show()
            binding.editMensaje.text.clear()
        }

        binding.btnSetting.setOnClickListener {
            Toast.makeText(this, "Configuración", Toast.LENGTH_SHORT).show()
        }
    }
}