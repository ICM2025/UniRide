package com.example.uniride.ui.driver.vehicles

import android.app.Activity
import android.app.AlertDialog
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.example.uniride.R
import com.example.uniride.databinding.FragmentEditVehicleBinding
import com.example.uniride.domain.model.VehicleBrand
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.io.File
import java.io.FileOutputStream

class EditVehicleFragment : Fragment() {

    private var _binding: FragmentEditVehicleBinding? = null
    private val binding get() = _binding!!

    private val db = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()

    private lateinit var selectedBrand: VehicleBrand
    private var currentCarId = ""
    private var currentBrand = ""
    private var currentModel = ""
    private var currentYear = 0
    private var currentColor = ""
    private var currentLicensePlate = ""
    private var currentImages = mutableListOf<String>()
    private var currentVehiclePhotoUri: Uri? = null
    private lateinit var uriCamera: Uri

    // Launcher para seleccionar imágenes de la galería
    private val getContentGallery =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let {
                uploadNewImage(it)
            }
        }

    // Launcher para tomar fotos con la cámara
    private val getContentCamera =
        registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            if (success) {
                uploadNewImage(uriCamera)
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Recuperar los argumentos pasados desde ManageVehiclesFragment
        arguments?.let {
            currentCarId = it.getString("CAR_ID", "")
            currentBrand = it.getString("BRAND", "")
            currentModel = it.getString("MODEL", "")
            currentYear = it.getInt("YEAR", 0)
            currentColor = it.getString("COLOR", "")
            currentLicensePlate = it.getString("PLATE", "")

            // Recuperar lista de imágenes
            val imagesList = it.getStringArrayList("IMAGES")
            if (imagesList != null) {
                currentImages = imagesList.toMutableList()
            }

            val imageUriStr = it.getString("IMAGE_URI")
            if (imageUriStr != null) {
                currentVehiclePhotoUri = Uri.parse(imageUriStr)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEditVehicleBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Configurar URI para la cámara
        val file = File(requireContext().filesDir, "vehiclePicFromCamera")
        uriCamera = FileProvider.getUriForFile(
            requireContext(),
            "${requireContext().packageName}.fileprovider",
            file
        )

        setupBrandSpinner()
        setupUI()
        setupListeners()

        // Cargar la primera imagen si existe en la lista (desde Firebase URL)
        if (currentImages.isNotEmpty()) {
            loadImageFromUrl(currentImages[0])
        } else if (currentVehiclePhotoUri != null) {
            // Fallback para imagen local
            loadImage(currentVehiclePhotoUri!!)
        } else {
            // Mostrar imagen placeholder si no hay imagen
            showPlaceholderImage()
        }
    }

    private fun setupBrandSpinner() {
        val brandNames = VehicleBrand.allBrands().map { it.displayName }
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, brandNames)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerBrand.adapter = adapter

        // Seleccionar la marca actual si existe
        if (currentBrand.isNotEmpty()) {
            val position = brandNames.indexOf(currentBrand)
            if (position != -1) {
                binding.spinnerBrand.setSelection(position)
                selectedBrand = VehicleBrand.fromDisplayName(currentBrand)
            }
        }

        binding.spinnerBrand.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                selectedBrand = VehicleBrand.fromDisplayName(parent.getItemAtPosition(position).toString())
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                selectedBrand = VehicleBrand.Otro
            }
        }
    }

    private fun setupUI() {
        binding.inputModel.setText(currentModel)
        binding.inputYear.setText(if (currentYear > 0) currentYear.toString() else "")
        binding.inputColor.setText(currentColor)
        binding.inputPlate.setText(currentLicensePlate)

        // Desactivar la edición de la placa ya que es el identificador único
        binding.inputPlate.isEnabled = false
    }

    private fun setupListeners() {
        binding.btnBack.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }

        binding.btnSave.setOnClickListener {
            saveVehicle()
        }

        binding.btnChangePhoto.setOnClickListener {
            showImagePickerDialog()
        }

        binding.ivVehiclePhoto.setOnClickListener {
            showImagePickerDialog()
        }
    }

    private fun showImagePickerDialog() {
        val options = arrayOf("Tomar foto", "Elegir de galería", "Cancelar")

        AlertDialog.Builder(requireContext())
            .setTitle("Seleccionar foto de vehículo")
            .setItems(options) { dialog, which ->
                when (which) {
                    0 -> getContentCamera.launch(uriCamera)
                    1 -> getContentGallery.launch("image/*")
                    2 -> dialog.dismiss()
                }
            }
            .show()
    }

    // Implementación con Glide - ¡Súper simple!
    private fun loadImageFromUrl(imageUrl: String) {
        if (imageUrl.isBlank()) {
            showPlaceholderImage()
            return
        }

        Glide.with(this)
            .load(imageUrl)
            .apply(
                RequestOptions()
                    .placeholder(R.drawable.ic_vehicle_placeholder) // Imagen mientras carga
                    .error(R.drawable.ic_car) // Imagen si hay error
                    .centerCrop() // Recortar imagen para que encaje
                    .diskCacheStrategy(DiskCacheStrategy.ALL) // Cache en disco
            )
            .into(binding.ivVehiclePhoto)

        binding.ivVehiclePhoto.visibility = View.VISIBLE
    }

    // Mostrar imagen placeholder cuando no hay imagen
    private fun showPlaceholderImage() {
        Glide.with(this)
            .load(R.drawable.ic_vehicle_placeholder)
            .into(binding.ivVehiclePhoto)

        binding.ivVehiclePhoto.visibility = View.VISIBLE
    }

    // Para cargar múltiples imágenes (si quieres implementar un carrusel)
    private fun loadAllImages() {
        if (currentImages.isNotEmpty()) {
            // Por ahora solo mostrar la primera imagen
            loadImageFromUrl(currentImages[0])

            // TODO: Si quieres mostrar todas las imágenes, puedes implementar un ViewPager o RecyclerView
        } else {
            showPlaceholderImage()
        }
    }

    private fun uploadNewImage(uri: Uri) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid == null) {
            Toast.makeText(requireContext(), "Error: sesión expirada", Toast.LENGTH_SHORT).show()
            return
        }

        // Mostrar loading
        binding.btnSave.isEnabled = false
        Toast.makeText(requireContext(), "Subiendo imagen...", Toast.LENGTH_SHORT).show()

        // Mostrar la imagen local inmediatamente para mejor UX
        loadImage(uri)

        val fileName = "img_${System.currentTimeMillis()}.jpg"
        val ref = storage.reference.child("cars/$uid/$currentCarId/$fileName")

        ref.putFile(uri)
            .continueWithTask { task ->
                if (!task.isSuccessful) throw task.exception ?: Exception("Upload failed")
                ref.downloadUrl
            }
            .addOnSuccessListener { downloadUrl ->
                // Agregar nueva imagen a la lista
                currentImages.add(downloadUrl.toString())

                // Actualizar la imagen con la URL de Firebase (opcional, ya que ya se muestra la local)
                // loadImageFromUrl(downloadUrl.toString())

                binding.btnSave.isEnabled = true
                Toast.makeText(requireContext(), "Imagen agregada exitosamente", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { exception ->
                binding.btnSave.isEnabled = true
                Toast.makeText(requireContext(), "Error al subir imagen: ${exception.message}", Toast.LENGTH_SHORT).show()

                // Recargar la imagen anterior si falla la subida
                if (currentImages.isNotEmpty()) {
                    loadImageFromUrl(currentImages[0])
                } else {
                    showPlaceholderImage()
                }
            }
    }

    // Método original para cargar imágenes locales (URI) - Ahora también con Glide
    private fun loadImage(uri: Uri) {
        Glide.with(this)
            .load(uri)
            .apply(
                RequestOptions()
                    .placeholder(R.drawable.ic_vehicle_placeholder)
                    .error(R.drawable.ic_car)
                    .centerCrop()
            )
            .into(binding.ivVehiclePhoto)

        binding.ivVehiclePhoto.visibility = View.VISIBLE
    }

    private fun saveVehicle() {
        val model = binding.inputModel.text.toString().trim()
        val year = binding.inputYear.text.toString().toIntOrNull()
        val color = binding.inputColor.text.toString().trim()

        if (model.isEmpty() || year == null || color.isEmpty()) {
            Toast.makeText(requireContext(), "Completa todos los campos", Toast.LENGTH_SHORT).show()
            return
        }

        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid == null) {
            Toast.makeText(requireContext(), "Error: sesión expirada", Toast.LENGTH_SHORT).show()
            return
        }

        // Mostrar loading
        binding.btnSave.isEnabled = false
        Toast.makeText(requireContext(), "Guardando cambios...", Toast.LENGTH_SHORT).show()

        // Crear el mapa de datos actualizados
        val updateData = hashMapOf<String, Any>(
            "brand" to selectedBrand.displayName,
            "model" to model,
            "year" to year,
            "color" to color,
            "images" to currentImages
        )

        // Actualizar en Firestore
        db.collection("Cars").document(currentCarId)
            .update(updateData)
            .addOnSuccessListener {
                binding.btnSave.isEnabled = true
                Toast.makeText(requireContext(), "Vehículo actualizado correctamente", Toast.LENGTH_SHORT).show()
                findNavController().popBackStack()
            }
            .addOnFailureListener { exception ->
                binding.btnSave.isEnabled = true
                Toast.makeText(requireContext(), "Error al actualizar vehículo: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}