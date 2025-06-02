package com.example.uniride.ui.driver.vehicles

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResult
import androidx.navigation.fragment.findNavController
import com.example.uniride.R
import com.example.uniride.databinding.FragmentAddVehicleBinding
import com.example.uniride.domain.model.Car
import com.example.uniride.domain.model.VehicleBrand
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage

class AddVehicleFragment : Fragment() {

    private var _binding: FragmentAddVehicleBinding? = null
    private val binding get() = _binding!!

    private val db = FirebaseFirestore.getInstance()


    private lateinit var selectedBrand: VehicleBrand
    // Para almacenar URIs de imgs seleccionadas
    private val imageUris = mutableListOf<Uri>()
    private val imageRequestLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            imageUris.add(it)
            //se agrega la imagen a la previsualización
            displayImages(it)
        }
    }





    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddVehicleBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupBrandSpinner()
        setupSaveButton()
        binding.btnBack.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }
        binding.btnAddImage.setOnClickListener {
            imageRequestLauncher.launch("image/*")
        }

    }

    private fun setupBrandSpinner() {
        val brandNames = VehicleBrand.allBrands().map { it.displayName }
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, brandNames)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerBrand.adapter = adapter

        binding.spinnerBrand.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                selectedBrand = VehicleBrand.fromDisplayName(parent.getItemAtPosition(position).toString())
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                selectedBrand = VehicleBrand.Otro
            }
        }
    }

    private fun setupSaveButton() {
        binding.btnSave.setOnClickListener {
            val uid = FirebaseAuth.getInstance().currentUser?.uid
            if (uid == null) {
                Toast.makeText(requireContext(), "Error: sesión expirada", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val validatedCar = validateForm(uid) ?: return@setOnClickListener

            uploadVehicleImages(uid, validatedCar.id, imageUris,
                //una vez suba todas las imagenes ejecuta onComplete
                onComplete = { imageUrls ->
                    //copia el carro pero reemplazando con las nuevas urls
                    val finalCar = validatedCar.copy(images = imageUrls)
                    saveCarToFirestore(finalCar)
                },
                onError = {
                    Toast.makeText(requireContext(), "Error al subir imágenes", Toast.LENGTH_SHORT).show()
                }
            )
        }
    }
    //valida campos completos en formulario del carro
    private fun validateForm(uid: String): Car? {
        val model = binding.inputModel.text.toString().trim()
        val year = binding.inputYear.text.toString().toIntOrNull()
        val color = binding.inputColor.text.toString().trim()
        val plate = binding.inputPlate.text.toString().trim()

        if (model.isEmpty() || year == null || color.isEmpty() || plate.isEmpty()) {
            Toast.makeText(requireContext(),
                "Completa todos los campos del formulario", Toast.LENGTH_SHORT).show()
            return null
        }

        if (imageUris.isEmpty()) {
            Toast.makeText(requireContext(),
                "Agrega al menos una imagen del vehículo", Toast.LENGTH_SHORT).show()
            return null
        }

        val carId = db.collection("Cars").document().id

        //crea el carro sin las imagenes ya que no se han subido a firebase aun
        return Car(
            id = carId,
            idDriver = uid,
            brand = selectedBrand.displayName,
            model = model,
            year = year,
            color = color,
            licensePlate = plate,
            images = emptyList()
        )
    }
    //sube las imagenes del carro a storage de firebase
    private fun uploadVehicleImages(
        uid: String,
        carId: String,
        imageUris: List<Uri>,
        onComplete: (List<String>) -> Unit,
        onError: () -> Unit
    ) {
        val storage = FirebaseStorage.getInstance()
        //links de las imagenes subidas
        val uploadedUrls = mutableListOf<String>()
        //indice de imagen actual a subir
        var uploadIndex = 0

        //recursión para subir todas las imagenes
        fun uploadNext() {
            //para cuando el indice indique que ya se subieron todas
            if (uploadIndex >= imageUris.size) {
                //ejecuta callback
                onComplete(uploadedUrls)
                return
            }

            val uri = imageUris[uploadIndex]
            //nombre segun hora
            val fileName = "img_${System.currentTimeMillis()}.jpg"
            //ruta de subida en storage
            val ref = storage.reference.child("cars/$uid/$carId/$fileName")

            //sube el archivo
            ref.putFile(uri)
                .continueWithTask { task ->
                    if (!task.isSuccessful) throw task.exception ?: Exception("Upload failed")
                    ref.downloadUrl
                }
                .addOnSuccessListener { url ->
                    uploadedUrls.add(url.toString())
                    uploadIndex++
                    uploadNext()
                }
                .addOnFailureListener {
                    onError()
                }
        }

        uploadNext()
    }



    //guardar el carro en firebase
    private fun saveCarToFirestore(car: Car) {
        db.collection("Cars").document(car.id)
            .set(car)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Vehículo registrado con éxito", Toast.LENGTH_SHORT).show()



                //si viene del formulario lo devuelve con el carro seleccionado
                if (findNavController().previousBackStackEntry?.destination?.id == R.id.publishRouteFragment) {
                    //para que el nuevo carro se seleccione automaticamente en el form
                    setFragmentResult("vehicle_added", bundleOf("newVehicleId" to car.id))
                    findNavController().navigate(R.id.action_addVehicleFragment_to_publishRouteFragment)
                } else {
                    //si viene de gestionar vehiculos crea el carro y se duelve
                    findNavController().popBackStack()
                }
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Error al guardar el vehículo", Toast.LENGTH_SHORT).show()
            }
    }





    //Para que se agreguen las imagenes a la previsualizacioón en el formulario
    private fun displayImages(uri: Uri) {
        val container = FrameLayout(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(220, 220).apply {
                setMargins(8, 8, 8, 8)
            }
        }

        val imageView = ImageView(requireContext()).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            setImageURI(uri)
            scaleType = ImageView.ScaleType.CENTER_CROP
            background = ContextCompat.getDrawable(requireContext(), R.drawable.bg_edittext_rounded)
            adjustViewBounds = true
        }


        container.addView(imageView)

        binding.imagesContainer.addView(container)
        //listener de cada imagen
        container.setOnClickListener {
            showImagePreviewDialog(uri)
        }

    }

    //preview de cada imagen en específico
    private fun showImagePreviewDialog(uri: Uri) {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_image_preview, null)
        val previewImageView = dialogView.findViewById<ImageView>(R.id.previewImageView)
        previewImageView.setImageURI(uri)

        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .create()

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.show()
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
