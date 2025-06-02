package com.example.uniride.ui.driver.profile

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
import androidx.navigation.fragment.findNavController
import com.example.uniride.R
import com.example.uniride.databinding.FragmentDriverProfileBinding
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
import javax.net.ssl.HttpsURLConnection

class DriverProfileFragment : Fragment() {

    private var _binding: FragmentDriverProfileBinding? = null
    private val binding get() = _binding!!

    private var currentName = ""
    private var currentEmail = ""
    private var currentPhone = ""
    private var currentUniversity = ""
    private var currentVehicleInfo = ""
    private var currentProfilePicUri: Uri? = null
    private lateinit var uriCamera: Uri

    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())


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

    private val editProfileLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.let { data ->
                currentName = data.getStringExtra("NAME") ?: currentName
                currentEmail = data.getStringExtra("EMAIL") ?: currentEmail
                currentPhone = data.getStringExtra("PHONE") ?: currentPhone

                data.getStringExtra("PROFILE_PIC_URI")?.let { uriString ->
                    currentProfilePicUri = Uri.parse(uriString)
                }
                updateProfileUI()
                saveProfileData()

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

        binding.btnManageVehicles.setOnClickListener {
            findNavController().navigate(R.id.manageVehiclesFragment)
        }

        binding.btnStatistics.setOnClickListener {
            findNavController().navigate(R.id.statisticsFragment)
        }

        binding.btnChangePicture.setOnClickListener {
            showImagePickerDialog()
        }

        binding.btnEditProfile.setOnClickListener {
            launchEditProfileActivity()
        }

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

    private fun loadFirebaseData() {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            val userId = currentUser.uid

            firestore.collection("users").document(userId)
                .get()
                .addOnSuccessListener { document ->
                    if (!isAdded || _binding == null) return@addOnSuccessListener

                    if (document.exists()) {
                        currentEmail = document.getString("email") ?: ""
                        currentName = document.getString("username") ?: ""

                        // NUEVO: Cargar imagen de perfil desde Firebase
                        val profileImageUrl = document.getString("imgProfile")
                        if (!profileImageUrl.isNullOrEmpty()) {
                            loadImageFromUrl(profileImageUrl)
                        }

                        if (currentEmail.isNotEmpty()) {
                            fetchUniversityFromEmail(currentEmail)
                        }

                        loadVehicleInfo(userId)
                        updateProfileUI()
                    }
                }
                .addOnFailureListener { exception ->
                    if (isAdded) {
                        Toast.makeText(requireContext(), "Error al cargar datos del usuario", Toast.LENGTH_SHORT).show()
                    }
                }
        }
    }

    private fun loadImageFromUrl(imageUrl: String) {
        // Usando corrutinas para cargar imagen desde URL
        scope.launch {
            try {
                withContext(Dispatchers.IO) {
                    val url = URL(imageUrl)
                    val connection = url.openConnection()
                    connection.doInput = true
                    connection.connect()
                    val inputStream = connection.getInputStream()
                    BitmapFactory.decodeStream(inputStream)
                }?.let { bitmap ->
                    if (isAdded && _binding != null) {
                        binding.ivProfile?.setImageBitmap(bitmap)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun loadVehicleInfo(userId: String) {
        firestore.collection("Cars")
            .whereEqualTo("idDriver", userId)
            .get()
            .addOnSuccessListener { documents ->
                // Verificar que el fragment aún está activo
                if (!isAdded || _binding == null) return@addOnSuccessListener

                if (!documents.isEmpty) {
                    val vehicle = documents.documents[0]
                    val brand = vehicle.getString("brand") ?: ""
                    val model = vehicle.getString("model") ?: ""
                    currentVehicleInfo = "$brand $model".trim()
                    updateProfileUI()
                }
            }
            .addOnFailureListener { exception ->
                if (isAdded) {
                    Toast.makeText(requireContext(), "Error al cargar información del vehículo", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun fetchUniversityFromEmail(email: String) {
        scope.launch {
            try {
                val domain = email.substringAfter("@")
                val university = getUniversityByDomain(domain)

                // Verificar que el fragment aún está activo antes de actualizar
                if (isAdded && _binding != null) {
                    currentUniversity = university
                    updateProfileUI()
                }
            } catch (e: Exception) {
                if (isAdded && _binding != null) {
                    currentUniversity = "Universidad no encontrada"
                    updateProfileUI()
                }
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

    private fun saveImageToLocal(uri: Uri) {
        try {
            val photoFile = File(requireContext().filesDir, "profile_pic_${System.currentTimeMillis()}.jpg")

            requireContext().contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(photoFile).use { output ->
                    input.copyTo(output)
                }
            }

            currentProfilePicUri = Uri.fromFile(photoFile)

            uploadProfileImageToFirebase(uri)

        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(requireContext(), "Error al guardar la imagen", Toast.LENGTH_SHORT).show()
        }
    }

    private fun uploadProfileImageToFirebase(uri: Uri) {
        val currentUser = auth.currentUser ?: return
        val fileName = "profile_${System.currentTimeMillis()}.jpg"
        val storageRef = storage.reference.child("profile_images/${currentUser.uid}/$fileName")

        storageRef.putFile(uri)
            .continueWithTask { task ->
                if (!task.isSuccessful) {
                    throw task.exception ?: Exception("Upload failed")
                }
                storageRef.downloadUrl
            }
            .addOnSuccessListener { downloadUrl ->
                // Guardar URL en Firestore
                saveProfileImageUrlToFirestore(downloadUrl.toString())
            }
            .addOnFailureListener { exception ->
                Toast.makeText(requireContext(), "Error al subir imagen: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun saveProfileImageUrlToFirestore(imageUrl: String) {
        val currentUser = auth.currentUser ?: return

        firestore.collection("users").document(currentUser.uid)
            .update("imgProfile", imageUrl)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Foto de perfil actualizada", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { exception ->
                Toast.makeText(requireContext(), "Error al actualizar foto: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun launchEditProfileActivity() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.activity_edit_profile, null)

        val dialog = Dialog(requireContext(), android.R.style.Theme_Material_Light_NoActionBar_Fullscreen)
        dialog.setContentView(dialogView)
        dialog.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)

        // Inicializar los componentes del layout
        val btnBack = dialogView.findViewById<ImageButton>(R.id.btnBackEditProfile)
        val ivProfilePic = dialogView.findViewById<ImageView>(R.id.ivProfilePicture)
        val btnChangePhoto = dialogView.findViewById<Button>(R.id.btnChangePhoto)
        val etName = dialogView.findViewById<EditText>(R.id.etName)
        val etEmail = dialogView.findViewById<EditText>(R.id.etEmail)
        val etPhone = dialogView.findViewById<EditText>(R.id.etPhone)
        val btnSave = dialogView.findViewById<Button>(R.id.btnSaveProfile)
        val btnCancel = dialogView.findViewById<Button>(R.id.btnCancel)

        // Configurar los valores actuales
        etName.setText(currentName)
        etEmail.setText(currentEmail)
        etPhone.setText(currentPhone)

        // Cargar la imagen de perfil actual
        currentProfilePicUri?.let { uri ->
            try {
                requireContext().contentResolver.openInputStream(uri)?.use { inputStream ->
                    val bitmap = BitmapFactory.decodeStream(inputStream)
                    ivProfilePic.setImageBitmap(bitmap)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // Configurar el botón para cambiar la foto
        btnChangePhoto.setOnClickListener {
            showImagePickerDialog()
            // Actualizar la imagen en el diálogo después de seleccionar una nueva
            currentProfilePicUri?.let { uri ->
                try {
                    requireContext().contentResolver.openInputStream(uri)?.use { inputStream ->
                        val bitmap = BitmapFactory.decodeStream(inputStream)
                        ivProfilePic.setImageBitmap(bitmap)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        // Configurar el botón de guardar
        btnSave.setOnClickListener {
            val name = etName.text.toString().trim()
            val email = etEmail.text.toString().trim()
            val phone = etPhone.text.toString().trim()

            if (name.isEmpty() || email.isEmpty() || phone.isEmpty()) {
                Toast.makeText(requireContext(), "Todos los campos son obligatorios", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                Toast.makeText(requireContext(), "El formato del email no es válido", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val currentUser = auth.currentUser
            if (currentUser != null) {
                // MODIFICADO: Agregar teléfono a las actualizaciones
                val updates = hashMapOf<String, Any>(
                    "username" to name,
                    "email" to email,
                    "phone" to phone  // NUEVO CAMPO
                )

                firestore.collection("users").document(currentUser.uid)
                    .update(updates)
                    .addOnSuccessListener {
                        currentName = name
                        currentEmail = email
                        currentPhone = phone

                        if (email != currentEmail) {
                            fetchUniversityFromEmail(email)
                        }

                        updateProfileUI()
                        saveProfileData()
                        Toast.makeText(requireContext(), "Perfil actualizado correctamente", Toast.LENGTH_SHORT).show()
                        dialog.dismiss()
                    }
                    .addOnFailureListener {
                        Toast.makeText(requireContext(), "Error al actualizar el perfil", Toast.LENGTH_SHORT).show()
                    }
            }
        }

        btnCancel.setOnClickListener { dialog.dismiss() }
        btnBack.setOnClickListener { dialog.dismiss() }

        dialog.show()
    }

    private fun loadProfileData() {
        val sharedPrefs = requireActivity().getSharedPreferences("user_profile", Activity.MODE_PRIVATE)

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
        // Verificar que el binding aún existe antes de actualizar la UI
        if (_binding == null) return

        try {
            // Usar safe calls para todas las vistas
            binding.tvName?.text = if (currentName.isNotEmpty()) currentName else "Nombre no disponible"
            binding.tvEmail?.text = if (currentEmail.isNotEmpty()) currentEmail else "Email no disponible"
            binding.tvUniversity?.text = if (currentUniversity.isNotEmpty()) currentUniversity else "Cargando universidad..."

            // Solo actualizar vehicle info si la vista existe
            binding.tvVehicleInfo?.text = if (currentVehicleInfo.isNotEmpty()) currentVehicleInfo else "Vehículo no disponible"

            // Cargar imagen de perfil de forma segura
            currentProfilePicUri?.let { uri ->
                try {
                    requireContext().contentResolver.openInputStream(uri)?.use { inputStream ->
                        val bitmap = BitmapFactory.decodeStream(inputStream)
                        binding.ivProfile?.setImageBitmap(bitmap)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    // No mostrar toast aquí para evitar spam de errores
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            // Log del error para debugging
            android.util.Log.e("DriverProfile", "Error updating UI: ${e.message}")
        }
    }

    private fun saveProfileData() {
        val sharedPrefs = requireActivity().getSharedPreferences("user_profile", Activity.MODE_PRIVATE)
        sharedPrefs.edit().apply {
            putString("NAME", currentName)
            putString("EMAIL", currentEmail)
            putString("PHONE", currentPhone)
            putString("UNIVERSITY", currentUniversity)
            putString("VEHICLE_INFO", currentVehicleInfo)
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