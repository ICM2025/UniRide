package com.example.uniride.ui.driver.profile

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.navigation.fragment.findNavController
import com.example.uniride.EditProfileActivity
import com.example.uniride.R
import com.example.uniride.databinding.FragmentDriverProfileBinding
import java.io.File
import java.io.FileOutputStream

class DriverProfileFragment : Fragment() {

    private var _binding: FragmentDriverProfileBinding? = null
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

    // Register for activity result to handle the edit profile activity result
    private val editProfileLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.let { data ->
                // Update profile information
                currentName = data.getStringExtra("NAME") ?: currentName
                currentEmail = data.getStringExtra("EMAIL") ?: currentEmail
                currentPhone = data.getStringExtra("PHONE") ?: currentPhone

                // Update profile picture if one was selected
                data.getStringExtra("PROFILE_PIC_URI")?.let { uriString ->
                    currentProfilePicUri = Uri.parse(uriString)
                }

                // Update UI with new information
                updateProfileUI()

                // Save the updated profile data
                saveProfileData()

                // Show success message
                Toast.makeText(requireContext(), "Perfil actualizado", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDriverProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Configure camera file
        val file = File(requireContext().filesDir, "picFromCamera")
        uriCamera = FileProvider.getUriForFile(
            requireContext(),
            "${requireContext().packageName}.fileprovider",
            file
        )

        // Load saved profile data
        loadProfileData()

        // Update UI with current data
        updateProfileUI()

        binding.btnBack.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }

        binding.btnManageVehicles.setOnClickListener {
            findNavController().navigate(R.id.manageVehiclesFragment)
        }

        binding.btnStatistics.setOnClickListener {
            findNavController().navigate(R.id.statisticsFragment)
        }

        binding.btnChangePicture.setOnClickListener {
            showImagePickerDialog()
        }

        // Add a click listener for the entire profile section
        binding.btnEditProfile.setOnClickListener {
            launchEditProfileActivity()
        }

        // Add click listener for profile picture to change it directly
        binding.ivProfile.setOnClickListener {
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
                binding.ivProfile.setImageBitmap(bitmap)
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
        val intent = Intent(requireContext(), EditProfileActivity::class.java).apply {
            putExtra("NAME", currentName)
            putExtra("EMAIL", currentEmail)
            putExtra("PHONE", currentPhone)
            // If you want to pass the current image URI
            currentProfilePicUri?.let { uri ->
                putExtra("PROFILE_PIC_URI", uri.toString())
            }
        }
        editProfileLauncher.launch(intent)
    }

    private fun loadProfileData() {
        // Here you would load the profile data from SharedPreferences, Database, etc.
        val sharedPrefs = requireActivity().getSharedPreferences("user_profile", Activity.MODE_PRIVATE)
        currentName = sharedPrefs.getString("NAME", "") ?: ""
        currentEmail = sharedPrefs.getString("EMAIL", "") ?: ""
        currentPhone = sharedPrefs.getString("PHONE", "") ?: ""

        // Load profile picture if exists
        val profilePicPath = sharedPrefs.getString("PROFILE_PIC_PATH", null)
        if (profilePicPath != null) {
            val file = File(profilePicPath)
            if (file.exists()) {
                currentProfilePicUri = Uri.fromFile(file)
            }
        }
    }

    private fun updateProfileUI() {
        // Update UI components with current profile data
        binding.tvName?.text = currentName

        // Update profile picture if available
        currentProfilePicUri?.let { uri ->
            try {
                requireContext().contentResolver.openInputStream(uri)?.use { inputStream ->
                    val bitmap = BitmapFactory.decodeStream(inputStream)
                    binding.ivProfile.setImageBitmap(bitmap)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun saveProfileData() {
        // Save profile data to SharedPreferences or your database
        val sharedPrefs = requireActivity().getSharedPreferences("user_profile", Activity.MODE_PRIVATE)
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
        // Save data when fragment is paused
        saveProfileData()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}