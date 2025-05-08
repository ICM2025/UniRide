package com.example.uniride.ui.passenger.profile

import android.app.Activity
import android.app.AlertDialog
import android.app.Dialog
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import com.example.uniride.R
import com.example.uniride.databinding.FragmentPassengerProfileBinding
import java.io.File
import java.io.FileOutputStream

class PassengerProfileFragment : Fragment() {

    private var _binding: FragmentPassengerProfileBinding? = null
    private val binding get() = _binding!!

    private var currentName = ""
    private var currentEmail = ""
    private var currentPhone = ""
    private var currentProfilePicUri: Uri? = null
    private lateinit var uriCamera: Uri

    // Launcher para seleccionar imágenes de la galería
    private val getContentGallery =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let {
                saveImageToLocal(it)
                loadImage(it)
                // Guardar los cambios
                saveProfileData()
                Toast.makeText(requireContext(), "Foto de perfil actualizada", Toast.LENGTH_SHORT).show()
            }
        }

    // Launcher para tomar fotos con la cámara
    private val getContentCamera =
        registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            if (success) {
                currentProfilePicUri = uriCamera
                loadImage(uriCamera)
                // Guardar los cambios
                saveProfileData()
                Toast.makeText(requireContext(), "Foto de perfil actualizada", Toast.LENGTH_SHORT).show()
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPassengerProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val file = File(requireContext().filesDir, "picFromCamera")
        uriCamera = FileProvider.getUriForFile(
            requireContext(),
            "${requireContext().packageName}.fileprovider",
            file
        )

        loadProfileData()
        updateProfileUI()

        binding.btnBack.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }

        binding.btnEditProfile.setOnClickListener {
            launchEditProfileActivity()
        }

        binding.ivProfile.setOnClickListener {
            showImagePickerDialog()
        }

        binding.btnChangePhoto.setOnClickListener {
            showImagePickerDialog()
        }
    }

    private fun showImagePickerDialog() {
        val options = arrayOf("Tomar foto", "Elegir de galería", "Cancelar")

        AlertDialog.Builder(requireContext())
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

    private fun loadImage(uri: Uri) {
        try {
            requireContext().contentResolver.openInputStream(uri)?.use { inputStream ->
                val bitmap = BitmapFactory.decodeStream(inputStream)
                binding.ivProfile?.setImageBitmap(bitmap)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(requireContext(), "Error al cargar la imagen", Toast.LENGTH_SHORT).show()
        }
    }

    private fun saveImageToLocal(uri: Uri) {
        try {
            // Crear un archivo local para guardar la imagen
            val photoFile = File(requireContext().filesDir, "profile_pic_${System.currentTimeMillis()}.jpg")

            // Copiar la imagen seleccionada al archivo local
            requireContext().contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(photoFile).use { output ->
                    input.copyTo(output)
                }
            }

            // Actualizar la URI de la imagen de perfil
            currentProfilePicUri = Uri.fromFile(photoFile)

        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(requireContext(), "Error al guardar la imagen", Toast.LENGTH_SHORT).show()
        }
    }

    private fun launchEditProfileActivity() {
        // Crear la vista para el fragmento de edición
        val editProfileView = LayoutInflater.from(requireContext())
            .inflate(R.layout.activity_edit_profile, null) as ViewGroup

        val dialog = Dialog(requireContext(), android.R.style.Theme_Material_Light_NoActionBar_Fullscreen)
        dialog.setContentView(editProfileView)
        dialog.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)

        // Inicializar vistas del diálogo
        val btnBack = editProfileView.findViewById<ImageButton>(R.id.btnBackEditProfile)
        val btnSave = editProfileView.findViewById<Button>(R.id.btnSaveProfile)
        val btnCancel = editProfileView.findViewById<Button>(R.id.btnCancel)
        val btnChangePhoto = editProfileView.findViewById<Button>(R.id.btnChangePhoto)
        val etName = editProfileView.findViewById<EditText>(R.id.etName)
        val etEmail = editProfileView.findViewById<EditText>(R.id.etEmail)
        val etPhone = editProfileView.findViewById<EditText>(R.id.etPhone)
        val ivProfilePicture = editProfileView.findViewById<ImageView>(R.id.ivProfilePicture)

        // Establecer los valores actuales
        etName.setText(currentName)
        etEmail.setText(currentEmail)
        etPhone.setText(currentPhone)

        // Cargar la imagen de perfil actual
        currentProfilePicUri?.let { uri ->
            try {
                requireContext().contentResolver.openInputStream(uri)?.use { inputStream ->
                    val bitmap = BitmapFactory.decodeStream(inputStream)
                    ivProfilePicture.setImageBitmap(bitmap)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // Configurar los botones
        btnBack.setOnClickListener { dialog.dismiss() }
        btnCancel.setOnClickListener { dialog.dismiss() }

        btnChangePhoto.setOnClickListener {
            showImagePickerDialog()
            // Actualizar la imagen en el diálogo cuando se cambie
            currentProfilePicUri?.let { uri ->
                try {
                    requireContext().contentResolver.openInputStream(uri)?.use { inputStream ->
                        val bitmap = BitmapFactory.decodeStream(inputStream)
                        ivProfilePicture.setImageBitmap(bitmap)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        btnSave.setOnClickListener {
            // Validar los campos
            val name = etName.text.toString().trim()
            val email = etEmail.text.toString().trim()
            val phone = etPhone.text.toString().trim()

            // Validación simple
            if (name.isEmpty() || email.isEmpty() || phone.isEmpty()) {
                Toast.makeText(requireContext(), "Todos los campos son obligatorios", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Validar formato de email
            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                Toast.makeText(requireContext(), "El formato del email no es válido", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Actualizar los datos
            currentName = name
            currentEmail = email
            currentPhone = phone

            // Actualizar UI y guardar datos
            updateProfileUI()
            saveProfileData()

            // Mostrar mensaje de éxito y cerrar el diálogo
            Toast.makeText(requireContext(), "Perfil actualizado correctamente", Toast.LENGTH_SHORT).show()
            dialog.dismiss()
        }

        // Mostrar el diálogo a pantalla completa
        dialog.show()
        dialog.window?.apply {
            setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        }
    }

    private fun loadProfileData() {
        val sharedPrefs = requireActivity().getSharedPreferences("passenger_profile", Activity.MODE_PRIVATE)
        currentName = sharedPrefs.getString("NAME", "") ?: ""
        currentEmail = sharedPrefs.getString("EMAIL", "") ?: ""
        currentPhone = sharedPrefs.getString("PHONE", "") ?: ""

        // Cargar la foto de perfil si existe
        val profilePicPath = sharedPrefs.getString("PROFILE_PIC_PATH", null)
        if (profilePicPath != null) {
            val file = File(profilePicPath)
            if (file.exists()) {
                currentProfilePicUri = Uri.fromFile(file)
            }
        }
    }

    private fun updateProfileUI() {
        binding.tvName?.text = currentName
        binding.tvEmail?.text = currentEmail

        currentProfilePicUri?.let { uri ->
            try {
                requireContext().contentResolver.openInputStream(uri)?.use { inputStream ->
                    val bitmap = BitmapFactory.decodeStream(inputStream)
                    binding.ivProfile?.setImageBitmap(bitmap)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun saveProfileData() {
        val sharedPrefs = requireActivity().getSharedPreferences("passenger_profile", Activity.MODE_PRIVATE)
        sharedPrefs.edit().apply {
            putString("NAME", currentName)
            putString("EMAIL", currentEmail)
            putString("PHONE", currentPhone)
            currentProfilePicUri?.let { uri ->
                putString("PROFILE_PIC_PATH", uri.path)
            }
            apply()
        }
    }

    override fun onPause() {
        super.onPause()
        saveProfileData()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}