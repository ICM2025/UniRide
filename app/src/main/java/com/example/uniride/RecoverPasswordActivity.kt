package com.example.uniride

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.uniride.databinding.ActivityRecoverPasswordBinding

class RecoverPasswordActivity : AppCompatActivity() {
    private lateinit var binding: ActivityRecoverPasswordBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRecoverPasswordBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.recoverButton.setOnClickListener{
            startActivity(Intent(this, NewPasswordActivity::class.java))
        }
    }
}