package com.example.uniride

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.uniride.databinding.ActivityRegisterBinding

class RegisterActivity : AppCompatActivity() {
    private lateinit var binding: ActivityRegisterBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.ingresarButton.setOnClickListener{
            startActivity(Intent(this, LoginActivity::class.java))
        }
        binding.createAccountButton.setOnClickListener{
            startActivity(Intent(this, BottomMenuActivity::class.java))
        }
    }
}