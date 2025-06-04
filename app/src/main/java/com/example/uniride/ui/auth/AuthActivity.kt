package com.example.uniride.ui.auth

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.example.uniride.R
import com.example.uniride.ui.MainActivity
import com.google.firebase.auth.FirebaseAuth

class AuthActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("AuthActivity", "Intent extras: ${intent.extras}")
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            // Ya ha iniciado sesión, redirigir a MainActivity
            val originalExtras = intent.extras
            val newIntent = Intent(this, MainActivity::class.java)
            if (originalExtras != null) {
                newIntent.putExtras(originalExtras)
            }
            startActivity(newIntent)
            finish()
            return
        }

        // Si no ha iniciado sesión, mostrar pantalla de login
        setContentView(R.layout.activity_auth)
    }
}