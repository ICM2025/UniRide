package com.example.uniride.ui.driver.trips

import android.app.AlertDialog
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.uniride.R
import com.example.uniride.databinding.FragmentTripRequestBinding
import com.example.uniride.domain.adapter.PassengerRequestsAdapter
import com.example.uniride.domain.model.PassengerRequestOLD
import com.example.uniride.domain.model.PassengerRequestStatus
import com.example.uniride.domain.model.PassengerRequest
import com.example.uniride.domain.model.RequestStatus
import com.example.uniride.domain.model.Trip
import com.example.uniride.domain.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

class TripRequestsFragment : Fragment() {

    private var _binding: FragmentTripRequestBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: PassengerRequestsAdapter

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private var requestsListener: ListenerRegistration? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTripRequestBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        loadPassengerRequests()
    }

    private fun setupRecyclerView() {
        binding.rvPassengerRequests.layoutManager = LinearLayoutManager(requireContext())
    }

    private fun loadPassengerRequests() {
        val currentDriverId = auth.currentUser?.uid
        if (currentDriverId == null) {
            Toast.makeText(requireContext(), "Error: Usuario no autenticado", Toast.LENGTH_SHORT)
                .show()
            return
        }

        // Primero obtener los trips del conductor actual
        db.collection("Trips")
            .whereEqualTo("idDriver", currentDriverId)
            .whereEqualTo("status", "PENDING") // Solo viajes activos
            .get()
            .addOnSuccessListener { tripsSnapshot ->
                val driverTripIds = tripsSnapshot.documents.mapNotNull { it.id }

                if (driverTripIds.isEmpty()) {
                    // No hay viajes del conductor, mostrar lista vacía
                    adapter = PassengerRequestsAdapter(
                        items = emptyList(),
                        onAccept = { },
                        onReject = { },
                        onClick = { }
                    )
                    binding.rvPassengerRequests.adapter = adapter
                    return@addOnSuccessListener
                }

                // Escuchar cambios en las solicitudes para los viajes del conductor
                requestsListener = db.collection("PassengerRequests")
                    .whereIn("idTrip", driverTripIds)
                    .addSnapshotListener { snapshot, error ->
                        if (error != null) {
                            Toast.makeText(
                                requireContext(),
                                "Error al cargar solicitudes",
                                Toast.LENGTH_SHORT
                            ).show()
                            return@addSnapshotListener
                        }

                        if (snapshot != null) {
                            val requests = snapshot.toObjects(PassengerRequest::class.java)
                            loadRequestsWithUserData(requests)
                        }
                    }
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Error al cargar viajes", Toast.LENGTH_SHORT)
                    .show()
            }
    }

    private fun loadRequestsWithUserData(requests: List<PassengerRequest>) {
        if (requests.isEmpty()) {
            adapter = PassengerRequestsAdapter(
                items = emptyList(),
                onAccept = { },
                onReject = { },
                onClick = { }
            )
            binding.rvPassengerRequests.adapter = adapter
            return
        }

        val requestsOLD = mutableListOf<PassengerRequestOLD>()
        var completedRequests = 0

        requests.forEach { request ->
            // Obtener datos del usuario para cada solicitud
            db.collection("users").document(request.idUser)
                .get()
                .addOnSuccessListener { userDoc ->
                    val user = userDoc.toObject(User::class.java)
                    if (user != null) {
                        // Obtener datos del viaje para mostrar destino
                        db.collection("Trips").document(request.idTrip)
                            .get()
                            .addOnSuccessListener { tripDoc ->
                                val trip = tripDoc.toObject(Trip::class.java)

                                val requestOLD = PassengerRequestOLD(
                                    passengerName = user.username,
                                    destination = "Destino del viaje", // Aquí podrías cargar el destino real
                                    status = when (request.status) {
                                        RequestStatus.PENDING -> PassengerRequestStatus.Pending
                                        RequestStatus.ACCEPTED -> PassengerRequestStatus.Accepted
                                        RequestStatus.REJECTED -> PassengerRequestStatus.Rejected
                                        RequestStatus.FINISHED -> PassengerRequestStatus.Finished
                                    },
                                    profileImg = R.drawable.ic_profile,
                                    university = "N.A",
                                    email = user.email,
                                    tripCount = 0, // Podrías calcular esto
                                    rating = 4.5, // Valor por defecto o calculado
                                    reviewsCount = 0, // Valor por defecto o calculado
                                    requestId = request.id // Agregar ID de la solicitud
                                )

                                requestsOLD.add(requestOLD)
                                completedRequests++

                                if (completedRequests == requests.size) {
                                    setupAdapter(requestsOLD)
                                }
                            }
                    } else {
                        completedRequests++
                        if (completedRequests == requests.size) {
                            setupAdapter(requestsOLD)
                        }
                    }
                }
                .addOnFailureListener {
                    completedRequests++
                    if (completedRequests == requests.size) {
                        setupAdapter(requestsOLD)
                    }
                }
        }
    }

    private fun setupAdapter(requests: List<PassengerRequestOLD>) {
        adapter = PassengerRequestsAdapter(
            items = requests,
            onAccept = { req -> acceptRequest(req) },
            onReject = { req -> rejectRequest(req) },
            onClick = { req -> showPassengerDetails(req) }
        )
        binding.rvPassengerRequests.adapter = adapter
    }

    private fun acceptRequest(request: PassengerRequestOLD) {
        request.requestId?.let { requestId ->
            db.collection("PassengerRequests").document(requestId)
                .update("status", RequestStatus.ACCEPTED)
                .addOnSuccessListener {
                    Toast.makeText(
                        requireContext(),
                        "Aceptaste a ${request.passengerName}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                .addOnFailureListener {
                    Toast.makeText(
                        requireContext(),
                        "Error al aceptar solicitud",
                        Toast.LENGTH_SHORT
                    ).show()
                }
        }
    }

    private fun rejectRequest(request: PassengerRequestOLD) {
        request.requestId?.let { requestId ->
            db.collection("PassengerRequests").document(requestId)
                .update("status", RequestStatus.REJECTED)
                .addOnSuccessListener {
                    Toast.makeText(
                        requireContext(),
                        "Rechazaste a ${request.passengerName}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                .addOnFailureListener {
                    Toast.makeText(
                        requireContext(),
                        "Error al rechazar solicitud",
                        Toast.LENGTH_SHORT
                    ).show()
                }
        }
    }

    private fun showPassengerDetails(request: PassengerRequestOLD) {
        PassengerRequestDetailBottomSheet(request) {
            Toast.makeText(requireContext(), "Abrir chat con ${request.passengerName}", Toast.LENGTH_SHORT).show()
            // Aquí puedes implementar la navegación al chat
        }.show(parentFragmentManager, "PassengerRequestDetail")
    }


    override fun onDestroyView() {
        super.onDestroyView()
        requestsListener?.remove()
        _binding = null
    }
}