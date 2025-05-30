package com.example.uniride.ui.driver.trips

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
import com.example.uniride.domain.model.PassengerRequest
import com.example.uniride.domain.model.PassengerRequestOLD
import com.example.uniride.domain.model.PassengerRequestStatus
import com.example.uniride.domain.model.RequestStatus
import com.example.uniride.domain.model.Route
import com.example.uniride.domain.model.Stop
import com.example.uniride.domain.model.Trip
import com.example.uniride.domain.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class TripRequestsFragment : Fragment() {

    private var _binding: FragmentTripRequestBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: PassengerRequestsAdapter
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

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
        val currentUserId = auth.currentUser?.uid
        if (currentUserId == null) {
            Toast.makeText(requireContext(), "Error: Usuario no identificado", Toast.LENGTH_SHORT).show()
            return
        }

        // Obtener los trips del conductor actual
        db.collection("Trips")
            .whereEqualTo("idDriver", currentUserId)
            .whereEqualTo("status", "PENDING")
            .get()
            .addOnSuccessListener { tripsSnapshot ->
                val tripIds = tripsSnapshot.documents.mapNotNull { it.id }

                if (tripIds.isEmpty()) {
                    setupAdapter(emptyList())
                    return@addOnSuccessListener
                }

                // Obtener las solicitudes de pasajeros para estos trips
                db.collection("PassengerRequests")
                    .whereIn("idTrip", tripIds)
                    .whereEqualTo("status", RequestStatus.PENDING.name)
                    .get()
                    .addOnSuccessListener { requestsSnapshot ->
                        val requests = mutableListOf<PassengerRequestOLD>()
                        var processedCount = 0
                        val totalCount = requestsSnapshot.size()

                        if (totalCount == 0) {
                            setupAdapter(emptyList())
                            return@addOnSuccessListener
                        }

                        requestsSnapshot.forEach { doc ->
                            val passengerRequest = doc.toObject(PassengerRequest::class.java).copy(id = doc.id)
                            convertToPassengerRequestOLD(passengerRequest) { requestOLD ->
                                if (requestOLD != null) {
                                    requests.add(requestOLD)
                                }
                                processedCount++
                                if (processedCount == totalCount) {
                                    setupAdapter(requests)
                                }
                            }
                        }
                    }
            }
    }

    private fun convertToPassengerRequestOLD(
        passengerRequest: PassengerRequest,
        onResult: (PassengerRequestOLD?) -> Unit
    ) {
        // Obtener datos del usuario pasajero
        db.collection("users").document(passengerRequest.idUser)
            .get()
            .addOnSuccessListener { userDoc ->
                val user = userDoc.toObject(User::class.java)
                if (user == null) {
                    onResult(null)
                    return@addOnSuccessListener
                }

                // Obtener datos del trip para obtener el destino
                db.collection("Trips").document(passengerRequest.idTrip)
                    .get()
                    .addOnSuccessListener { tripDoc ->
                        val trip = tripDoc.toObject(Trip::class.java)
                        if (trip == null) {
                            onResult(null)
                            return@addOnSuccessListener
                        }

                        // Obtener ruta para obtener destino
                        db.collection("Routes").document(trip.idRoute)
                            .get()
                            .addOnSuccessListener { routeDoc ->
                                val route = routeDoc.toObject(Route::class.java)
                                if (route == null) {
                                    onResult(null)
                                    return@addOnSuccessListener
                                }

                                // Obtener stop de destino
                                db.collection("Stops").document(route.idDestination)
                                    .get()
                                    .addOnSuccessListener { stopDoc ->
                                        val destinationStop = stopDoc.toObject(Stop::class.java)

                                        val requestOLD = PassengerRequestOLD(
                                            passengerName = user.username,
                                            destination = destinationStop?.name ?: "Destino desconocido",
                                            status = PassengerRequestStatus.Pending,
                                            profileImg = R.drawable.ic_profile,
                                            university = "N/A",
                                            email = user.email,
                                            tripCount = 0, // Puedes implementar esto más tarde
                                            rating = 0.0, // Puedes implementar esto más tarde
                                            reviewsCount = 0 // Puedes implementar esto más tarde
                                        )

                                        onResult(requestOLD)
                                    }
                            }
                    }
            }
    }

    private fun setupAdapter(requests: List<PassengerRequestOLD>) {
        adapter = PassengerRequestsAdapter(
            items = requests,
            onAccept = { req -> acceptPassengerRequest(req) },
            onReject = { req -> rejectPassengerRequest(req) },
            onClick = { req -> showPassengerDetails(req) }
        )
        binding.rvPassengerRequests.adapter = adapter
    }

    private fun acceptPassengerRequest(request: PassengerRequestOLD) {
        // Encontrar la solicitud real en Firebase usando el nombre del pasajero
        findPassengerRequestByName(request.passengerName) { passengerRequest ->
            if (passengerRequest != null) {
                // Actualizar status a ACCEPTED
                db.collection("PassengerRequests").document(passengerRequest.id)
                    .update("status", RequestStatus.ACCEPTED.name)
                    .addOnSuccessListener {
                        // Decrementar cupos disponibles del trip
                        decrementTripSeats(passengerRequest.idTrip) {
                            Toast.makeText(requireContext(), "Aceptaste a ${request.passengerName}", Toast.LENGTH_SHORT).show()
                            loadPassengerRequests() // Recargar lista
                        }
                    }
                    .addOnFailureListener {
                        Toast.makeText(requireContext(), "Error al aceptar solicitud", Toast.LENGTH_SHORT).show()
                    }
            }
        }
    }

    private fun rejectPassengerRequest(request: PassengerRequestOLD) {
        findPassengerRequestByName(request.passengerName) { passengerRequest ->
            if (passengerRequest != null) {
                // Actualizar status a REJECTED
                db.collection("PassengerRequests").document(passengerRequest.id)
                    .update("status", RequestStatus.REJECTED.name)
                    .addOnSuccessListener {
                        Toast.makeText(requireContext(), "Rechazaste a ${request.passengerName}", Toast.LENGTH_SHORT).show()
                        loadPassengerRequests() // Recargar lista
                    }
                    .addOnFailureListener {
                        Toast.makeText(requireContext(), "Error al rechazar solicitud", Toast.LENGTH_SHORT).show()
                    }
            }
        }
    }

    private fun findPassengerRequestByName(passengerName: String, onResult: (PassengerRequest?) -> Unit) {
        // Buscar usuario por nombre
        db.collection("users")
            .whereEqualTo("username", passengerName)
            .get()
            .addOnSuccessListener { usersSnapshot ->
                if (usersSnapshot.isEmpty) {
                    onResult(null)
                    return@addOnSuccessListener
                }

                val userId = usersSnapshot.documents.first().id

                // Buscar solicitud pendiente de este usuario
                db.collection("PassengerRequests")
                    .whereEqualTo("idUser", userId)
                    .whereEqualTo("status", RequestStatus.PENDING.name)
                    .get()
                    .addOnSuccessListener { requestsSnapshot ->
                        if (requestsSnapshot.isEmpty) {
                            onResult(null)
                        } else {
                            val doc = requestsSnapshot.documents.first()
                            val request = doc.toObject(PassengerRequest::class.java)?.copy(id = doc.id)
                            onResult(request)
                        }
                    }
            }
    }

    private fun decrementTripSeats(tripId: String, onComplete: () -> Unit) {
        db.collection("Trips").document(tripId)
            .get()
            .addOnSuccessListener { doc ->
                val trip = doc.toObject(Trip::class.java)
                if (trip != null && trip.places > 0) {
                    db.collection("Trips").document(tripId)
                        .update("places", trip.places - 1)
                        .addOnSuccessListener { onComplete() }
                        .addOnFailureListener { onComplete() }
                } else {
                    onComplete()
                }
            }
    }

    private fun showPassengerDetails(request: PassengerRequestOLD) {
        PassengerRequestDetailBottomSheet(request) {
            Toast.makeText(requireContext(), "Abrir chat con ${request.passengerName}", Toast.LENGTH_SHORT).show()
            // acá se lanza actividad para el chat
        }.show(parentFragmentManager, "PassengerRequestDetail")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
