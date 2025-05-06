package com.example.uniride

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.uniride.databinding.ActivityAcercaDeBinding

class AcercaDeActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAcercaDeBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super. onCreate(savedInstanceState)
        binding = ActivityAcercaDeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Listener para el botón de retroceso
        binding.btnBack.setOnClickListener {
            finish()
        }
    }
}
