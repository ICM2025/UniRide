package com.example.uniride.ui.driver.vehicles

import android.app.Activity
import android.app.AlertDialog
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
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.uniride.R
import com.example.uniride.databinding.FragmentManageVehiclesBinding
import com.example.uniride.domain.adapter.VehicleAdapter
import com.example.uniride.domain.model.Vehicle
import java.io.File
import java.io.FileOutputStream

class ManageVehiclesFragment : Fragment() {

    private var _binding: FragmentManageVehiclesBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: VehicleAdapter
    private var currentVehicleList = mutableListOf<Vehicle>()
    private var currentVehiclePhotoUri: Uri? = null
    private lateinit var uriCamera: Uri

    // Launcher para seleccionar imágenes de la galería
    private val getContentGallery =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let {
                saveImageToLocal(it)
                // Actualizar el listado y guardar los datos
                saveVehicleData()
                Toast.makeText(requireContext(), "Foto de vehículo actualizada", Toast.LENGTH_SHORT).show()
            }
        }

    // Launcher para tomar fotos con la cámara
    private val getContentCamera =
        registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            if (success) {
                currentVehiclePhotoUri = uriCamera
                // Actualizar el listado y guardar los datos
                saveVehicleData()
                Toast.makeText(requireContext(), "Foto de vehículo actualizada", Toast.LENGTH_SHORT).show()
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentManageVehiclesBinding.inflate(inflater, container, false)
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

        setupRecyclerView()
        setupListeners()
        loadVehicleData()
    }

    private fun setupRecyclerView() {
        adapter = VehicleAdapter(
            onItemClick = { vehicle ->
                // Abrir fragmento de edición con los datos del vehículo seleccionado
                val bundle = Bundle().apply {
                    putString("BRAND", vehicle.brand)
                    putString("MODEL", vehicle.model)
                    putInt("YEAR", vehicle.year)
                    putString("COLOR", vehicle.color)
                    putString("PLATE", vehicle.licensePlate)
                    // Si hay imágenes, pasar la primera
                    if (vehicle.imageUrls.isNotEmpty()) {
                        putString("IMAGE_URI", vehicle.imageUrls[0])
                    }
                }
                findNavController().navigate(R.id.action_manageVehiclesFragment_to_editVehicleFragment, bundle)
            },
            onTakePhotoClick = { vehicle ->
                // Guardar el vehículo actual para asociar la foto
                currentVehiclePhotoUri = null
                showImagePickerDialog(vehicle)
            }
        )
        binding.rvVehicles.layoutManager = LinearLayoutManager(requireContext())
        binding.rvVehicles.adapter = adapter
    }

    private fun loadVehicleData() {
        val sharedPrefs = requireActivity().getSharedPreferences("vehicles_data", Activity.MODE_PRIVATE)
        val vehiclesJson = sharedPrefs.getString("VEHICLES", null)

        if (vehiclesJson != null) {
            currentVehicleList = mutableListOf(
                Vehicle("Toyota", "Corolla", 2020, "Blanco", "ABC123"),
                Vehicle("Mazda", "3", 2022, "Gris", "XYZ987")
            )
        } else {
            // Si no hay datos guardados, usar lista de ejemplo
            currentVehicleList = mutableListOf(
                Vehicle("Toyota", "Corolla", 2020, "Blanco", "ABC123"),
                Vehicle("Mazda", "3", 2022, "Gris", "XYZ987")
            )
        }

        adapter.submitList(currentVehicleList)
    }

    private fun setupListeners() {
        binding.btnBack.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }

        binding.btnAddVehicle.setOnClickListener {
            findNavController().navigate(R.id.action_manageVehiclesFragment_to_addVehicleFragment3)
        }
    }

    private fun showImagePickerDialog(vehicle: Vehicle) {
        val options = arrayOf("Tomar foto", "Elegir de galería", "Cancelar")

        AlertDialog.Builder(requireContext())
            .setTitle("Seleccionar foto de vehículo")
            .setItems(options) { dialog, which ->
                when (which) {
                    0 -> {
                        getContentCamera.launch(uriCamera)
                        saveVehicleWithPhoto(vehicle, uriCamera.toString())
                    }
                    1 -> {
                        getContentGallery.launch("image/*")
                    }
                    2 -> dialog.dismiss()
                }
            }
            .show()
    }

    private fun saveImageToLocal(uri: Uri): Uri {
        try {
            val photoFile = File(requireContext().filesDir, "vehicle_pic_${System.currentTimeMillis()}.jpg")

            requireContext().contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(photoFile).use { output ->
                    input.copyTo(output)
                }
            }

            return Uri.fromFile(photoFile)

        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(requireContext(), "Error al guardar la imagen", Toast.LENGTH_SHORT).show()
            return uri
        }
    }

    private fun saveVehicleWithPhoto(vehicle: Vehicle, imageUri: String) {
        val index = currentVehicleList.indexOfFirst {
            it.licensePlate == vehicle.licensePlate
        }

        if (index != -1) {
            val imageUrls = if (vehicle.imageUrls.isEmpty()) {
                listOf(imageUri)
            } else {
                val mutableList = vehicle.imageUrls.toMutableList()
                mutableList.add(imageUri)
                mutableList
            }

            val updatedVehicle = vehicle.copy(imageUrls = imageUrls)

            currentVehicleList[index] = updatedVehicle
            adapter.submitList(currentVehicleList.toList())
            saveVehicleData()
        }
    }

    private fun saveVehicleData() {
        // En una implementación real, guardaríamos la lista de vehículos en SharedPreferences o en una base de datos
        val sharedPrefs = requireActivity().getSharedPreferences("vehicles_data", Activity.MODE_PRIVATE)

        sharedPrefs.edit().apply {
            putString("VEHICLES", "saved_data")
            apply()
        }
    }

    override fun onPause() {
        super.onPause()
        saveVehicleData()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}