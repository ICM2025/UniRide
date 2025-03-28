package com.example.uniride

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.uniride.databinding.ActivityLoginBinding

class LoginActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLoginBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.registerButton.setOnClickListener{
            startActivity(Intent(this, RegisterActivity::class.java))
        }
        binding.ingresarButton.setOnClickListener{
            startActivity(Intent(this, PassengerHomeActivity::class.java))
        }
        binding.olvidoText.setOnClickListener{
            startActivity(Intent(this, RecoverPasswordActivity::class.java))
        }
    }
}