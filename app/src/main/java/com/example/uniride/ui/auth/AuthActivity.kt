package com.example.uniride.ui.auth

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.uniride.R
import com.example.uniride.ui.MainActivity
import com.google.firebase.auth.FirebaseAuth

class AuthActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            // Ya ha iniciado sesión → redirigir a MainActivity
            val intent = Intent(this, MainActivity::class.java)
            intent.putExtras(intent) // reenviar extras como destinationFromNotification
            startActivity(intent)
            finish()
            return
        }

        // Si no ha iniciado sesión → mostrar pantalla de login
        setContentView(R.layout.activity_auth)
    }
}