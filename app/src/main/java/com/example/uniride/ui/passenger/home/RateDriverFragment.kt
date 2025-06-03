package com.example.uniride.ui.passenger.home

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup


import android.widget.Toast
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.bumptech.glide.Glide
import com.example.uniride.databinding.FragmentRateDriverBinding
import com.example.uniride.domain.model.DriverStats
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class RateDriverFragment : Fragment() {

    private var _binding: FragmentRateDriverBinding? = null
    private val binding get() = _binding!!
    private val args: RateDriverFragmentArgs by navArgs()

    private val db = FirebaseFirestore.getInstance()
    private var idDriver: String? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRateDriverBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val tripId = args.tripId

        // Paso 1: obtener idDriver
        db.collection("Trips").document(tripId).get()
            .addOnSuccessListener { tripDoc ->
                if (tripDoc.exists()) {
                    idDriver = tripDoc.getString("idDriver")
                    idDriver?.let { loadDriverProfile(it) }
                } else {
                    Toast.makeText(requireContext(), "No se encontró el viaje", Toast.LENGTH_SHORT).show()
                    findNavController().popBackStack()
                }
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Error al cargar datos del viaje", Toast.LENGTH_SHORT).show()
                findNavController().popBackStack()
            }

        binding.btnSubmitRating.setOnClickListener {
            submitRating()
        }

        binding.ratingBar.setOnRatingBarChangeListener { _, rating, _ ->
            val label = when (rating.toInt()) {
                1 -> "Muy malo"
                2 -> "Malo"
                3 -> "Regular"
                4 -> "Bueno"
                5 -> "Excelente"
                else -> ""
            }
            binding.tvRatingLabel.text = label
        }


    }

    private fun loadDriverProfile(driverId: String) {
        db.collection("users").document(driverId).get()
            .addOnSuccessListener { userDoc ->
                val name = userDoc.getString("username") ?: "Conductor"
                val imageUrl = userDoc.getString("imgProfile")

                binding.tvDriverName.text = name

                if (!imageUrl.isNullOrEmpty()) {
                    Glide.with(this)
                        .load(imageUrl)
                        .into(binding.imgDriverProfile)
                }
            }
    }

    private fun submitRating() {
        val stars = binding.ratingBar.rating.toInt()
        val comment = binding.etComment.text.toString().trim()
        val tripId = args.tripId
        val idPassenger = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val driverId = idDriver ?: return

        if (stars == 0) {
            Toast.makeText(requireContext(), "Selecciona una cantidad de estrellas", Toast.LENGTH_SHORT).show()
            return
        }

        val ratingData = hashMapOf(
            "idTrip" to tripId,
            "idDriver" to driverId,
            "idPassenger" to idPassenger,
            "stars" to stars,
            "comment" to comment,
            "timestamp" to Timestamp.now()
        )

        db.collection("DriverRatings").add(ratingData)
            .addOnSuccessListener {
                updateDriverStats(driverId, stars)
                Toast.makeText(requireContext(), "Calificación enviada", Toast.LENGTH_SHORT).show()
                findNavController().popBackStack()
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Error al guardar calificación", Toast.LENGTH_SHORT).show()
            }
    }

    private fun updateDriverStats(driverId: String, newRating: Int) {
        val statsRef = db.collection("DriverStats").document(driverId)

        statsRef.get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    val stats = doc.toObject(DriverStats::class.java)
                    if (stats != null) {
                        val oldAvg = stats.rating
                        val oldCount = stats.passengersTransported
                        val newAvg = ((oldAvg * oldCount) + newRating) / (oldCount + 1)

                        statsRef.update(
                            mapOf(
                                "passengersTransported" to oldCount + 1,
                                "rating" to newAvg
                            )
                        )
                    }
                } else {
                    // Crear el documento si no existe
                    statsRef.set(
                        DriverStats(
                            idDriver = driverId,
                            tripsPublished = 0,
                            passengersTransported = 1,
                            rating = newRating.toDouble()
                        )
                    )
                }
            }
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
