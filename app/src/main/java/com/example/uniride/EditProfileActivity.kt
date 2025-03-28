package com.example.uniride

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.uniride.databinding.ActivityEditProfileBinding

class EditProfileActivity : AppCompatActivity() {
    private lateinit var binding: ActivityEditProfileBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupButtonListeners()
        loadInitialProfileData()
    }

    private fun setupButtonListeners() {
        binding.btnBackEditProfile.setOnClickListener {
            finish()
        }

        binding.btnCancel.setOnClickListener {
            finish()
        }

        binding.btnSaveProfile.setOnClickListener {
            saveProfileChanges()
        }

        binding.btnChangePhoto.setOnClickListener {
            Toast.makeText(this, "Cambio de foto pendiente", Toast.LENGTH_SHORT).show()
        }
    }


    private fun loadInitialProfileData() {
        binding.etName.setText(intent.getStringExtra("NAME") ?: "")
        binding.etEmail.setText(intent.getStringExtra("EMAIL") ?: "")
        binding.etPhone.setText(intent.getStringExtra("PHONE") ?: "")
    }

    private fun saveProfileChanges() {
        val name = binding.etName.text.toString().trim()
        val email = binding.etEmail.text.toString().trim()
        val phone = binding.etPhone.text.toString().trim()

        if (name.isEmpty()) {
            binding.etName.error = "Nombre es requerido"
            return
        }

        if (email.isEmpty()) {
            binding.etEmail.error = "Correo es requerido"
            return
        }


        Toast.makeText(this, "Perfil guardado", Toast.LENGTH_SHORT).show()

        val resultIntent = Intent().apply {
            putExtra("NAME", name)
            putExtra("EMAIL", email)
            putExtra("PHONE", phone)
        }
        setResult(Activity.RESULT_OK, resultIntent)
        finish()
    }
}