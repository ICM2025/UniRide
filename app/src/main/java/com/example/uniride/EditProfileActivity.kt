package com.example.uniride

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import com.example.uniride.databinding.ActivityEditProfileBinding
import java.io.File

class EditProfileActivity : AppCompatActivity() {
    private lateinit var binding: ActivityEditProfileBinding
    lateinit var uriCamera : Uri

    private val getContentGallery =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let { loadImage(it) }
        }

    val getContentCamera = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) {
            loadImage(uriCamera)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val file = File(filesDir, "picFromCamera")
        uriCamera = FileProvider.getUriForFile(this, "$packageName.fileprovider", file)

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
            showImagePickerDialog()
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

    private fun showImagePickerDialog() {
        val options = arrayOf("Tomar foto", "Elegir de galería", "Cancelar")

        AlertDialog.Builder(this)
            .setTitle("Seleccionar foto de perfil")
            .setItems(options) { dialog, which ->
                when (which) {
                    0 -> getContentCamera.launch(uriCamera)
                    1 -> getContentGallery.launch("image/*")
                    2 -> dialog.dismiss()
                }
            }
            .show()
    }

    fun loadImage(uri: Uri){
        try {
            contentResolver.openInputStream(uri)?.use { inputStream ->
                val bitmap = BitmapFactory.decodeStream(inputStream)
                binding.ivProfilePicture.setImageBitmap(bitmap)
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Error al cargar la imagen", Toast.LENGTH_SHORT).show()
        }
    }
}