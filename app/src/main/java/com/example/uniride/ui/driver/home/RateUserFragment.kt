package com.example.uniride.ui.driver.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.uniride.R
import com.example.uniride.databinding.FragmentRateUserBinding
import com.example.uniride.domain.adapter.PassengerRatingAdapter
import com.example.uniride.domain.model.front.PassengerRatingItem
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore

class RateUserFragment : Fragment() {

    private lateinit var binding: FragmentRateUserBinding
    private lateinit var adapter: PassengerRatingAdapter
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val args: RateUserFragmentArgs by navArgs()

    private val ratingItems = mutableListOf<PassengerRatingItem>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentRateUserBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        loadAcceptedPassengers()

        binding.btnSubmitRatings.setOnClickListener {
            submitRatings()
        }
    }

    private fun setupRecyclerView() {
        adapter = PassengerRatingAdapter(ratingItems)
        binding.recyclerViewRatings.adapter = adapter
        binding.recyclerViewRatings.layoutManager = LinearLayoutManager(requireContext())
    }

    private fun loadAcceptedPassengers() {
        val tripId = args.tripId
        db.collection("PassengerRequests")
            .whereEqualTo("idTrip", tripId)
            .whereEqualTo("status", "ACCEPTED")
            .get()
            .addOnSuccessListener { requests ->
                val acceptedIds = requests.mapNotNull { it.getString("idUser") }

                if (acceptedIds.isEmpty()) {
                    Toast.makeText(requireContext(), "No hay pasajeros para calificar", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }

                db.collection("users")
                    .whereIn(FieldPath.documentId(), acceptedIds)
                    .get()
                    .addOnSuccessListener { users ->
                        ratingItems.clear()
                        for (doc in users.documents) {
                            val id = doc.id
                            val name = doc.getString("username") ?: "Pasajero"
                            val profileUrl = doc.getString("imgProfile")


                            ratingItems.add(
                                PassengerRatingItem(
                                    idUser = id,
                                    name = name,
                                    profileUrl = profileUrl
                                )
                            )
                        }
                        adapter.notifyDataSetChanged()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(requireContext(), "Error al cargar usuarios", Toast.LENGTH_SHORT).show()
                    }
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Error al cargar pasajeros", Toast.LENGTH_SHORT).show()
            }
    }


    private fun submitRatings() {
        val ratings = adapter.getRatings()
        val idDriver = auth.currentUser?.uid ?: return
        val tripId = args.tripId

        if (ratings.any { it.stars == 0 }) {
            Toast.makeText(requireContext(), "Todos los pasajeros deben tener una calificación", Toast.LENGTH_SHORT).show()
            return
        }

        db.runTransaction { transaction ->
            // Primero recolectar los snapshots de todos los UserStats
            val snapshots = mutableMapOf<String, com.google.firebase.firestore.DocumentSnapshot>()
            for (item in ratings) {
                val statsRef = db.collection("UserStats").document(item.idUser)
                val snapshot = transaction.get(statsRef)
                snapshots[item.idUser] = snapshot
            }

            // escrotira de calificaciones
            for (item in ratings) {
                val statsRef = db.collection("UserStats").document(item.idUser)
                val snapshot = snapshots[item.idUser]!!
                val newRating = item.stars

                if (snapshot.exists()) {
                    val oldAvg = snapshot.getDouble("rating") ?: 0.0
                    val rawCount = snapshot.get("tripsTaken")
                    val oldCount = when (rawCount) {
                        is Number -> rawCount.toLong()
                        is String -> rawCount.toLongOrNull() ?: 0L
                        else -> 0L
                    }

                    val newAvg = ((oldAvg * oldCount) + newRating) / (oldCount + 1)

                    transaction.update(statsRef, mapOf(
                        "rating" to newAvg,
                        "tripsTaken" to oldCount + 1
                    ))
                } else {
                    transaction.set(statsRef, mapOf(
                        "idUser" to item.idUser,
                        "rating" to newRating.toDouble(),
                        "tripsTaken" to 1
                    ))
                }
            }
        }.addOnSuccessListener {

            // Guardar ratings
            val ratingCollection = db.collection("UserRatings")
            var completed = 0
            for (item in ratings) {
                val ratingData = mapOf(
                    "idTrip" to tripId,
                    "idPassenger" to item.idUser,
                    "idDriver" to idDriver,
                    "stars" to item.stars,
                    "timestamp" to Timestamp.now()
                )
                ratingCollection.add(ratingData)
                    .addOnSuccessListener {
                        completed++
                        if (completed == ratings.size) {
                            Toast.makeText(requireContext(), "Calificaciones enviadas", Toast.LENGTH_SHORT).show()
                            findNavController().navigate(R.id.action_rateUserFragment_to_driverHomeFragment)
                        }
                    }
                    .addOnFailureListener { e ->
                    }
            }
        }.addOnFailureListener { e ->
            Toast.makeText(requireContext(), "Error al guardar calificaciones", Toast.LENGTH_SHORT).show()
        }
    }



}

