package com.example.uniride.ui.driver.home

import android.net.Uri
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.navigation.fragment.findNavController
import com.example.uniride.R
import com.example.uniride.databinding.FragmentRegisterDriverBinding
import com.example.uniride.domain.model.Driver
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.util.Date

class RegisterDriverFragment : Fragment() {

    private var _binding: FragmentRegisterDriverBinding? = null
    private val binding get() = _binding!!

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    private var selectedImageUri: Uri? = null

    private val pickImageLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            selectedImageUri = uri
            binding.ivLicensePreview.setImageURI(uri)
        }
    }



    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRegisterDriverBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        binding.btnConfirmRegister.setOnClickListener {
            val uid = auth.currentUser?.uid

            if (uid == null) {
                Toast.makeText(requireContext(), "Error: Usuario no autenticado",
                    Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (selectedImageUri == null) {
                Toast.makeText(requireContext(), "Debes subir la imagen de tu licencia",
                    Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            uploadLicenseImage { imageUrl ->
                val driver = Driver(
                    id = uid,
                    imgLicense = imageUrl,
                    createdAt = Date()
                )

                firestore.collection("drivers").document(uid).set(driver)
                    .addOnSuccessListener {
                        Toast.makeText(requireContext(), "Registro como conductor exitoso",
                            Toast.LENGTH_SHORT).show()
                        //una vez se registra se va al home de driver
                        findNavController()
                            .navigate(R.id.action_registerDriverFragment_to_driverHomeFragment)
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(requireContext(), "Error al registrar: ${e.message}",
                            Toast.LENGTH_LONG).show()
                    }
            }
        }


        binding.btnUploadLicense.setOnClickListener {
            pickImageLauncher.launch("image/*")
        }


        binding.btnBack.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }
    }

    private fun uploadLicenseImage(onUrlReady: (String) -> Unit) {
        val uid = auth.currentUser?.uid ?: return
        val imageRef = FirebaseStorage.getInstance().reference.child("license_images/$uid.jpg")

        imageRef.putFile(selectedImageUri!!)
            .addOnSuccessListener {
                imageRef.downloadUrl.addOnSuccessListener { uri ->
                    onUrlReady(uri.toString())
                }
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Error al subir la imagen", Toast.LENGTH_SHORT).show()
            }
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
