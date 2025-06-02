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
import com.bumptech.glide.Glide
import com.example.uniride.R
import com.example.uniride.databinding.FragmentPassengerProfileBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.io.File
import java.io.FileOutputStream
import java.net.URL
import java.util.UUID
import javax.net.ssl.HttpsURLConnection

class PassengerProfileFragment : Fragment() {

    private var _binding: FragmentPassengerProfileBinding? = null
    private val binding get() = _binding!!

    private var currentName = ""
    private var currentEmail = ""
    private var currentPhone = ""
    private var currentUniversity = ""
    private var currentProfilePicUri: Uri? = null
    private var currentProfilePicUrl = ""
    private lateinit var uriCamera: Uri

    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    // Launcher para seleccionar imágenes de la galería
    private val getContentGallery =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let {
                uploadImageToFirebase(it) { success ->
                    if (success) {
                        saveImageToLocal(it)
                        loadImage(it)
                        Toast.makeText(requireContext(), "Foto de perfil actualizada", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(requireContext(), "Error al subir la imagen", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

    // Launcher para tomar fotos con la cámara
    private val getContentCamera =
        registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            if (success) {
                currentProfilePicUri = uriCamera
                uploadImageToFirebase(uriCamera) { uploadSuccess ->
                    if (uploadSuccess) {
                        loadImage(uriCamera)
                        Toast.makeText(requireContext(), "Foto de perfil actualizada", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(requireContext(), "Error al subir la imagen", Toast.LENGTH_SHORT).show()
                    }
                }
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

        loadFirebaseData()
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

    private fun uploadImageToFirebase(imageUri: Uri, callback: (Boolean) -> Unit) {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            callback(false)
            return
        }

        // Crear referencia única para la imagen
        val imageRef = storage.reference
            .child("profile_images")
            .child("${currentUser.uid}_${UUID.randomUUID()}.jpg")

        // Subir imagen
        imageRef.putFile(imageUri)
            .addOnSuccessListener { taskSnapshot ->
                // Obtener URL de descarga
                imageRef.downloadUrl.addOnSuccessListener { downloadUri ->
                    currentProfilePicUrl = downloadUri.toString()

                    // Actualizar en Firestore
                    firestore.collection("users").document(currentUser.uid)
                        .update("imgProfile", currentProfilePicUrl)
                        .addOnSuccessListener {
                            callback(true)
                        }
                        .addOnFailureListener {
                            callback(false)
                        }
                }.addOnFailureListener {
                    callback(false)
                }
            }
            .addOnFailureListener {
                callback(false)
            }
    }


    private fun loadFirebaseData() {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            val userId = currentUser.uid

            // Cargar datos del usuario
            firestore.collection("users").document(userId)
                .get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        currentEmail = document.getString("email") ?: ""
                        currentName = document.getString("username") ?: ""
                        currentPhone = document.getString("phone") ?: ""
                        currentProfilePicUrl = document.getString("imgProfile") ?: ""

                        // Obtener información de la universidad basada en el email
                        if (currentEmail.isNotEmpty()) {
                            fetchUniversityFromEmail(currentEmail)
                        }

                        updateProfileUI()
                    }
                }
                .addOnFailureListener { exception ->
                    Toast.makeText(requireContext(), "Error al cargar datos del usuario", Toast.LENGTH_SHORT).show()
                }
        }
    }


    private fun fetchUniversityFromEmail(email: String) {
        scope.launch {
            try {
                val domain = email.substringAfter("@")
                val university = getUniversityByDomain(domain)
                currentUniversity = university
                updateProfileUI()
            } catch (e: Exception) {
                currentUniversity = "Universidad no encontrada"
                updateProfileUI()
            }
        }
    }

    private suspend fun getUniversityByDomain(domain: String): String = withContext(Dispatchers.IO) {
        try {
            val url = URL("https://raw.githubusercontent.com/Hipo/university-domains-list/master/world_universities_and_domains.json")
            val connection = url.openConnection() as HttpsURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 10000
            connection.readTimeout = 10000

            val response = connection.inputStream.bufferedReader().use { it.readText() }
            val jsonArray = JSONArray(response)

            for (i in 0 until jsonArray.length()) {
                val university = jsonArray.getJSONObject(i)
                val alphaCode = university.optString("alpha_two_code", "")

                if (alphaCode == "CO") {
                    val domains = university.getJSONArray("domains")
                    for (j in 0 until domains.length()) {
                        if (domains.getString(j).equals(domain, ignoreCase = true)) {
                            return@withContext university.getString("name")
                        }
                    }
                }
            }

            return@withContext "Universidad no encontrada"
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext "Error al obtener universidad"
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
        val editProfileView = LayoutInflater.from(requireContext())
            .inflate(R.layout.activity_edit_profile, null) as ViewGroup

        val dialog = Dialog(requireContext(), android.R.style.Theme_Material_Light_NoActionBar_Fullscreen)
        dialog.setContentView(editProfileView)
        dialog.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)

        val btnBack = editProfileView.findViewById<ImageButton>(R.id.btnBackEditProfile)
        val btnSave = editProfileView.findViewById<Button>(R.id.btnSaveProfile)
        val btnCancel = editProfileView.findViewById<Button>(R.id.btnCancel)
        val btnChangePhoto = editProfileView.findViewById<Button>(R.id.btnChangePhoto)
        val etName = editProfileView.findViewById<EditText>(R.id.etName)
        val etEmail = editProfileView.findViewById<EditText>(R.id.etEmail)
        val etPhone = editProfileView.findViewById<EditText>(R.id.etPhone)
        val ivProfilePicture = editProfileView.findViewById<ImageView>(R.id.ivProfilePicture)

        // Cargar datos actuales
        etName.setText(currentName)
        etEmail.setText(currentEmail)
        etPhone.setText(currentPhone)

        // Cargar imagen de perfil
        loadProfileImage(ivProfilePicture)

        btnBack.setOnClickListener { dialog.dismiss() }
        btnCancel.setOnClickListener { dialog.dismiss() }

        btnChangePhoto.setOnClickListener {
            showImagePickerDialogForEdit(ivProfilePicture)
        }

        btnSave.setOnClickListener {
            val name = etName.text.toString().trim()
            val email = etEmail.text.toString().trim()
            val phone = etPhone.text.toString().trim()

            if (name.isEmpty() || email.isEmpty()) {
                Toast.makeText(requireContext(), "El nombre y email son obligatorios", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                Toast.makeText(requireContext(), "El formato del email no es válido", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Actualizar datos en Firebase
            updateUserProfile(name, email, phone) { success ->
                if (success) {
                    currentName = name
                    currentEmail = email
                    currentPhone = phone

                    // Actualizar universidad si cambió el email
                    if (email != currentEmail) {
                        fetchUniversityFromEmail(email)
                    }

                    updateProfileUI()
                    saveProfileData()
                    Toast.makeText(requireContext(), "Perfil actualizado correctamente", Toast.LENGTH_SHORT).show()
                    dialog.dismiss()
                } else {
                    Toast.makeText(requireContext(), "Error al actualizar el perfil", Toast.LENGTH_SHORT).show()
                }
            }
        }

        dialog.show()
        dialog.window?.apply {
            setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        }
    }

    private fun updateUserProfile(name: String, email: String, phone: String, callback: (Boolean) -> Unit) {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            val updates = hashMapOf<String, Any>(
                "username" to name,
                "email" to email,
                "phone" to phone
            )

            firestore.collection("users").document(currentUser.uid)
                .update(updates)
                .addOnSuccessListener {
                    callback(true)
                }
                .addOnFailureListener {
                    callback(false)
                }
        } else {
            callback(false)
        }
    }

    private fun showImagePickerDialogForEdit(imageView: ImageView) {
        val options = arrayOf("Tomar foto", "Elegir de galería", "Cancelar")

        AlertDialog.Builder(requireContext())
            .setTitle("Seleccionar foto de perfil")
            .setItems(options) { dialog, which ->
                when (which) {
                    0 -> {
                        // Tomar foto con cámara
                        registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
                            if (success) {
                                uploadImageToFirebase(uriCamera) { uploadSuccess ->
                                    if (uploadSuccess) {
                                        loadImageIntoView(uriCamera, imageView)
                                        loadImage(uriCamera) // También actualizar la imagen principal
                                        Toast.makeText(requireContext(), "Foto actualizada", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        }.launch(uriCamera)
                    }
                    1 -> {
                        // Elegir de galería
                        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
                            uri?.let {
                                uploadImageToFirebase(it) { uploadSuccess ->
                                    if (uploadSuccess) {
                                        loadImageIntoView(it, imageView)
                                        loadImage(it) // También actualizar la imagen principal
                                        Toast.makeText(requireContext(), "Foto actualizada", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        }.launch("image/*")
                    }
                    2 -> dialog.dismiss()
                }
            }
            .show()
    }

    private fun loadImageIntoView(uri: Uri, imageView: ImageView) {
        try {
            requireContext().contentResolver.openInputStream(uri)?.use { inputStream ->
                val bitmap = BitmapFactory.decodeStream(inputStream)
                imageView.setImageBitmap(bitmap)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun loadProfileImage(imageView: ImageView) {
        when {
            currentProfilePicUrl.isNotEmpty() -> {
                // Cargar desde URL de Firebase (usando Glide si está disponible)
                try {
                    Glide.with(requireContext())
                        .load(currentProfilePicUrl)
                        .into(imageView)
                } catch (e: Exception) {
                    // Si Glide no está disponible, usar método alternativo
                    // Podrías implementar tu propio cargador de imágenes aquí
                    imageView.setImageResource(R.drawable.ic_passenger)
                }
            }
            currentProfilePicUri != null -> {
                // Cargar desde URI local
                loadImageIntoView(currentProfilePicUri!!, imageView)
            }
            else -> {
                // Imagen por defecto
                imageView.setImageResource(R.drawable.ic_passenger)
            }
        }
    }

    private fun loadProfileData() {
        val sharedPrefs = requireActivity().getSharedPreferences("passenger_profile", Activity.MODE_PRIVATE)

        // Solo cargar datos locales si no hay datos de Firebase
        if (currentName.isEmpty()) {
            currentName = sharedPrefs.getString("NAME", "") ?: ""
        }
        if (currentEmail.isEmpty()) {
            currentEmail = sharedPrefs.getString("EMAIL", "") ?: ""
        }
        if (currentPhone.isEmpty()) {
            currentPhone = sharedPrefs.getString("PHONE", "") ?: ""
        }

        val profilePicPath = sharedPrefs.getString("PROFILE_PIC_PATH", null)
        if (profilePicPath != null) {
            val file = File(profilePicPath)
            if (file.exists()) {
                currentProfilePicUri = Uri.fromFile(file)
            }
        }
    }

    private fun updateProfileUI() {
        binding.tvName?.text = if (currentName.isNotEmpty()) currentName else "Nombre no disponible"
        binding.tvEmail?.text = if (currentEmail.isNotEmpty()) currentEmail else "Email no disponible"
        binding.tvUniversity?.text = if (currentUniversity.isNotEmpty()) currentUniversity else "Cargando universidad..."

        // Cargar imagen de perfil
        when {
            currentProfilePicUrl.isNotEmpty() -> {
                try {
                    Glide.with(requireContext())
                        .load(currentProfilePicUrl)
                        .into(binding.ivProfile)
                } catch (e: Exception) {
                    // Si Glide no está disponible, usar imagen por defecto
                    binding.ivProfile?.setImageResource(R.drawable.ic_passenger)
                }
            }
            currentProfilePicUri != null -> {
                loadImage(currentProfilePicUri!!)
            }
            else -> {
                binding.ivProfile?.setImageResource(R.drawable.ic_passenger)
            }
        }
    }

    private fun saveProfileData() {
        val sharedPrefs = requireActivity().getSharedPreferences("passenger_profile", Activity.MODE_PRIVATE)
        sharedPrefs.edit().apply {
            putString("NAME", currentName)
            putString("EMAIL", currentEmail)
            putString("PHONE", currentPhone)
            putString("UNIVERSITY", currentUniversity)
            putString("PROFILE_PIC_URL", currentProfilePicUrl)
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
        scope.cancel()
        _binding = null
    }
}