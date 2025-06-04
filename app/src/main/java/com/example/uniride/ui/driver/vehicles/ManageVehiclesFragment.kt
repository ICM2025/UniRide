package com.example.uniride.ui.driver.vehicles

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
import com.example.uniride.domain.model.Car
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.io.File

class ManageVehiclesFragment : Fragment() {

    private var _binding: FragmentManageVehiclesBinding? = null
    private val binding get() = _binding!!

    private val db = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()

    private lateinit var adapter: VehicleAdapter
    private var currentVehicleList = mutableListOf<Car>()
    private var selectedCarForPhoto: Car? = null
    private lateinit var uriCamera: Uri
    private var isLoading = false

    // Launcher para seleccionar imágenes de la galería
    private val getContentGallery =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let {
                selectedCarForPhoto?.let { car ->
                    uploadImageForCar(car, it)
                }
            }
        }

    // Launcher para tomar fotos con la cámara
    private val getContentCamera =
        registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            if (success) {
                selectedCarForPhoto?.let { car ->
                    uploadImageForCar(car, uriCamera)
                }
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
        loadVehiclesFromFirebase()
    }

    private fun setupRecyclerView() {
        adapter = VehicleAdapter(
            onItemClick = { car ->
                // Convertir Car a Bundle para EditVehicleFragment
                val bundle = Bundle().apply {
                    putString("CAR_ID", car.id)
                    putString("BRAND", car.brand)
                    putString("MODEL", car.model)
                    putInt("YEAR", car.year)
                    putString("COLOR", car.color)
                    putString("PLATE", car.licensePlate)
                    // Pasar lista de imágenes
                    putStringArrayList("IMAGES", ArrayList(car.images))
                    // Si hay imágenes, pasar la primera para mostrar
                    if (car.images.isNotEmpty()) {
                        putString("IMAGE_URI", car.images[0])
                    }
                }
                findNavController().navigate(R.id.action_manageVehiclesFragment_to_editVehicleFragment, bundle)
            },
            onTakePhotoClick = { car ->
                selectedCarForPhoto = car
                showImagePickerDialog()
            },
            onDeleteClick = { car ->
                showDeleteConfirmationDialog(car)
            }
        )
        binding.rvVehicles.layoutManager = LinearLayoutManager(requireContext())
        binding.rvVehicles.adapter = adapter
    }

    private fun loadVehiclesFromFirebase() {
        if (isLoading) return // Prevenir cargas múltiples

        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid == null) {
            Toast.makeText(requireContext(), "Error: sesión expirada", Toast.LENGTH_SHORT).show()
            return
        }

        isLoading = true

        db.collection("Cars")
            .whereEqualTo("idDriver", uid)
            .get()
            .addOnSuccessListener { documents ->
                val newVehicleList = mutableListOf<Car>()

                for (document in documents) {
                    try {
                        val car = document.toObject(Car::class.java)
                        // Asegurarse de que el ID del documento se asigne correctamente
                        if (car.id.isEmpty()) {
                            car.id = document.id
                        }
                        newVehicleList.add(car)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

                currentVehicleList.clear()
                currentVehicleList.addAll(newVehicleList)
                adapter.submitList(ArrayList(currentVehicleList))

                updateUIVisibility()
                isLoading = false
            }
            .addOnFailureListener { exception ->
                isLoading = false
                Toast.makeText(requireContext(), "Error al cargar vehículos: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }
    private fun updateUIVisibility() {
        if (currentVehicleList.isEmpty()) {
            binding.rvVehicles.visibility = View.GONE
        } else {
            binding.rvVehicles.visibility = View.VISIBLE
        }
    }


    private fun setupListeners() {
        binding.btnBack.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }

        binding.btnAddVehicle.setOnClickListener {
            findNavController().navigate(R.id.action_manageVehiclesFragment_to_addVehicleFragment3)
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
                    2 -> {
                        selectedCarForPhoto = null
                        dialog.dismiss()
                    }
                }
            }
            .show()
    }

    private fun uploadImageForCar(car: Car, imageUri: Uri) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid == null) {
            Toast.makeText(requireContext(), "Error: sesión expirada", Toast.LENGTH_SHORT).show()
            return
        }

        Toast.makeText(requireContext(), "Subiendo imagen...", Toast.LENGTH_SHORT).show()

        val fileName = "img_${System.currentTimeMillis()}.jpg"
        val ref = storage.reference.child("cars/$uid/${car.id}/$fileName")

        ref.putFile(imageUri)
            .continueWithTask { task ->
                if (!task.isSuccessful) throw task.exception ?: Exception("Upload failed")
                ref.downloadUrl
            }
            .addOnSuccessListener { downloadUrl ->
                // Actualizar la lista de imágenes del carro
                val updatedImages = car.images.toMutableList()
                updatedImages.add(downloadUrl.toString())

                // Actualizar en Firestore
                db.collection("Cars").document(car.id)
                    .update("images", updatedImages)
                    .addOnSuccessListener {
                        Toast.makeText(requireContext(), "Imagen agregada exitosamente", Toast.LENGTH_SHORT).show()

                        // Actualizar solo este vehículo en la lista local (más eficiente)
                        val index = currentVehicleList.indexOfFirst { it.id == car.id }
                        if (index != -1) {
                            currentVehicleList[index].images = updatedImages
                            adapter.notifyItemChanged(index)
                        }

                        selectedCarForPhoto = null
                    }
                    .addOnFailureListener {
                        Toast.makeText(requireContext(), "Error al actualizar vehículo", Toast.LENGTH_SHORT).show()
                        selectedCarForPhoto = null
                    }
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Error al subir imagen", Toast.LENGTH_SHORT).show()
                selectedCarForPhoto = null
            }
    }

    private fun showDeleteConfirmationDialog(car: Car) {
        AlertDialog.Builder(requireContext())
            .setTitle("Eliminar vehículo")
            .setMessage("¿Estás seguro de que deseas eliminar este vehículo? Esta acción no se puede deshacer.")
            .setPositiveButton("Eliminar") { _, _ ->
                deleteVehicle(car)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun deleteVehicle(car: Car) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid == null) {
            Toast.makeText(requireContext(), "Error: sesión expirada", Toast.LENGTH_SHORT).show()
            return
        }

        Toast.makeText(requireContext(), "Eliminando vehículo...", Toast.LENGTH_SHORT).show()

        // Eliminar imágenes del storage
        car.images.forEach { imageUrl ->
            try {
                val imageRef = storage.getReferenceFromUrl(imageUrl)
                imageRef.delete()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // Eliminar documento de Firestore
        db.collection("Cars").document(car.id)
            .delete()
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Vehículo eliminado exitosamente", Toast.LENGTH_SHORT).show()
                // Recargar la lista
                loadVehiclesFromFirebase()
            }
            .addOnFailureListener { exception ->
                Toast.makeText(requireContext(), "Error al eliminar vehículo: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onResume() {
        super.onResume()
        loadVehiclesFromFirebase()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}