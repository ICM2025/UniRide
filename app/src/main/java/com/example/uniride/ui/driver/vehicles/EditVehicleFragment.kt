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
import com.example.uniride.R
import com.example.uniride.databinding.FragmentEditVehicleBinding
import com.example.uniride.domain.model.VehicleBrand
import java.io.File
import java.io.FileOutputStream

class EditVehicleFragment : Fragment() {

    private var _binding: FragmentEditVehicleBinding? = null
    private val binding get() = _binding!!

    private lateinit var selectedBrand: VehicleBrand
    private var currentBrand = ""
    private var currentModel = ""
    private var currentYear = 0
    private var currentColor = ""
    private var currentLicensePlate = ""
    private var currentVehiclePhotoUri: Uri? = null
    private lateinit var uriCamera: Uri

    // Launcher para seleccionar imágenes de la galería
    private val getContentGallery =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let {
                saveImageToLocal(it)
                loadImage(it)
                Toast.makeText(requireContext(), "Foto de vehículo actualizada", Toast.LENGTH_SHORT).show()
            }
        }

    // Launcher para tomar fotos con la cámara
    private val getContentCamera =
        registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            if (success) {
                currentVehiclePhotoUri = uriCamera
                loadImage(uriCamera)
                Toast.makeText(requireContext(), "Foto de vehículo actualizada", Toast.LENGTH_SHORT).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Recuperar los argumentos pasados desde ManageVehiclesFragment
        arguments?.let {
            currentBrand = it.getString("BRAND", "")
            currentModel = it.getString("MODEL", "")
            currentYear = it.getInt("YEAR", 0)
            currentColor = it.getString("COLOR", "")
            currentLicensePlate = it.getString("PLATE", "")

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

        // Cargar la imagen si existe
        currentVehiclePhotoUri?.let {
            loadImage(it)
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

    private fun loadImage(uri: Uri) {
        try {
            requireContext().contentResolver.openInputStream(uri)?.use { inputStream ->
                val bitmap = BitmapFactory.decodeStream(inputStream)
                binding.ivVehiclePhoto.setImageBitmap(bitmap)
                binding.ivVehiclePhoto.visibility = View.VISIBLE
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(requireContext(), "Error al cargar la imagen", Toast.LENGTH_SHORT).show()
        }
    }

    private fun saveImageToLocal(uri: Uri) {
        try {
            // Crear un archivo local para guardar la imagen
            val photoFile = File(requireContext().filesDir, "vehicle_pic_${System.currentTimeMillis()}.jpg")

            // Copiar la imagen seleccionada al archivo local
            requireContext().contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(photoFile).use { output ->
                    input.copyTo(output)
                }
            }

            // Actualizar la URI de la imagen de perfil
            currentVehiclePhotoUri = Uri.fromFile(photoFile)

        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(requireContext(), "Error al guardar la imagen", Toast.LENGTH_SHORT).show()
        }
    }

    private fun saveVehicle() {
        val model = binding.inputModel.text.toString().trim()
        val year = binding.inputYear.text.toString().toIntOrNull()
        val color = binding.inputColor.text.toString().trim()
        val plate = binding.inputPlate.text.toString().trim()

        if (model.isEmpty() || year == null || color.isEmpty() || plate.isEmpty()) {
            Toast.makeText(requireContext(), "Completa todos los campos", Toast.LENGTH_SHORT).show()
            return
        }

        currentBrand = selectedBrand.displayName
        currentModel = model
        currentYear = year
        currentColor = color

        saveVehicleData()

        Toast.makeText(requireContext(), "Vehículo actualizado correctamente", Toast.LENGTH_SHORT).show()
        findNavController().popBackStack()
    }

    private fun saveVehicleData() {
        val sharedPrefs = requireActivity().getSharedPreferences("vehicles_data", Activity.MODE_PRIVATE)

        sharedPrefs.edit().apply {
            putString("VEHICLE_UPDATED", "true")
            apply()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}