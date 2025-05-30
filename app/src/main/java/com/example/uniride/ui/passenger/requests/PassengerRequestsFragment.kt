package com.example.uniride.ui.passenger.requests

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.uniride.R
import com.example.uniride.databinding.FragmentPassengerRequestsBinding
import com.example.uniride.domain.adapter.TravelRequestAdapter
import com.example.uniride.domain.model.*
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore
import java.time.LocalDate
import java.time.LocalDateTime

class PassengerRequestsFragment : Fragment() {

    private var _binding: FragmentPassengerRequestsBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: TravelRequestAdapter
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPassengerRequestsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.rvRequests.layoutManager = LinearLayoutManager(requireContext())

        // Mostrar loading si es necesario
        loadPassengerRequests()
    }

    private fun loadPassengerRequests() {
        val db = FirebaseFirestore.getInstance()
        val currentUserId = getCurrentUserId()

        if (currentUserId.isNullOrEmpty()) {
            Toast.makeText(requireContext(), "Error: Usuario no identificado", Toast.LENGTH_SHORT).show()
            return
        }

        db.collection("PassengerRequests")
            .whereEqualTo("idUser", currentUserId)
            .get()
            .addOnSuccessListener { snapshot ->
                val requests = mutableListOf<TravelRequest>()
                var processedCount = 0
                val totalCount = snapshot.size()

                if (totalCount == 0) {
                    setupAdapter(emptyList())
                    return@addOnSuccessListener
                }

                snapshot.forEach { doc ->
                    val passengerRequest = doc.toObject(PassengerRequest::class.java)

                    // Cargar los datos del Trip
                    db.collection("Trips").document(passengerRequest.idTrip)
                        .get()
                        .addOnSuccessListener { tripDoc ->
                            val trip = tripDoc.toObject(Trip::class.java)
                            if (trip != null) {
                                convertToTravelRequest(trip, passengerRequest) { travelRequest ->
                                    if (travelRequest != null) {
                                        requests.add(travelRequest)
                                    }
                                    processedCount++
                                    if (processedCount == totalCount) {
                                        setupAdapter(requests)
                                    }
                                }
                            } else {
                                processedCount++
                                if (processedCount == totalCount) {
                                    setupAdapter(requests)
                                }
                            }
                        }
                }
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Error al cargar solicitudes", Toast.LENGTH_SHORT).show()
            }
    }

    private fun convertToTravelRequest(
        trip: Trip,
        passengerRequest: PassengerRequest,
        onReady: (TravelRequest?) -> Unit
    ) {
        val db = FirebaseFirestore.getInstance()

        // Cargar Route
        db.collection("Routes").document(trip.idRoute)
            .get()
            .addOnSuccessListener { routeDoc ->
                val route = routeDoc.toObject(Route::class.java)
                if (route == null) {
                    onReady(null)
                    return@addOnSuccessListener
                }

                // Cargar stops origen y destino
                val originTask = db.collection("Stops").document(route.idOrigin).get()
                val destinationTask = db.collection("Stops").document(route.idDestination).get()
                val userTask = db.collection("users").document(trip.idDriver).get()

                Tasks.whenAllSuccess<DocumentSnapshot>(originTask, destinationTask, userTask)
                    .addOnSuccessListener { docs ->
                        val originStop = docs[0].toObject(Stop::class.java)
                        val destinationStop = docs[1].toObject(Stop::class.java)
                        val driver = docs[2].toObject(User::class.java)

                        if (originStop != null && destinationStop != null && driver != null) {
                            // Cargar paradas intermedias
                            db.collection("StopsInRoute")
                                .whereEqualTo("idRoute", route.id)
                                .get()
                                .addOnSuccessListener { stopsInRouteSnap ->
                                    val stopIds = stopsInRouteSnap.mapNotNull { it.getString("idStop") }

                                    if (stopIds.isEmpty()) {
                                        createTravelRequest(trip, passengerRequest, originStop, destinationStop, driver, emptyList(), onReady)
                                    } else {
                                        db.collection("Stops")
                                            .whereIn(FieldPath.documentId(), stopIds)
                                            .get()
                                            .addOnSuccessListener { stopsSnap ->
                                                val intermediateStops = stopsSnap.toObjects(Stop::class.java)
                                                    .filter { it.name != originStop.name && it.name != destinationStop.name }
                                                createTravelRequest(trip, passengerRequest, originStop, destinationStop, driver, intermediateStops, onReady)
                                            }
                                    }
                                }
                        } else {
                            onReady(null)
                        }
                    }
            }
    }

    private fun createTravelRequest(
        trip: Trip,
        passengerRequest: PassengerRequest,
        originStop: Stop,
        destinationStop: Stop,
        driver: User,
        intermediateStops: List<Stop>,
        onReady: (TravelRequest) -> Unit
    ) {
        val travelOption = TravelOption(
            driverName = driver.username,
            description = "Viaje disponible",
            price = trip.price,
            driverImage = R.drawable.ic_profile,
            drawableResId = R.drawable.ic_car,
            availableSeats = trip.places,
            origin = originStop.name,
            destination = destinationStop.name,
            departureTime = trip.startTime,
            intermediateStops = listOf(originStop.name) + intermediateStops.map { it.name } + listOf(destinationStop.name),
            travelDate = LocalDate.parse(trip.date)
        )

        val status = when (passengerRequest.status) {
            RequestStatus.PENDING -> TravelRequestStatus.Pending
            RequestStatus.ACCEPTED -> TravelRequestStatus.Accepted
            RequestStatus.REJECTED -> TravelRequestStatus.Rejected
            RequestStatus.FINISHED -> TravelRequestStatus.Finished
        }

        val travelRequest = TravelRequest(
            travelOption = travelOption,
            requestDate = LocalDateTime.now(), // Puedes usar la fecha de creación real si la guardas
            status = status
        )

        onReady(travelRequest)
    }

    private fun setupAdapter(requests: List<TravelRequest>) {
        adapter = TravelRequestAdapter(requests) { selectedRequest ->
            val bottomSheet = RequestDetailBottomSheet(selectedRequest)
            bottomSheet.show(parentFragmentManager, "RequestDetail")
        }
        binding.rvRequests.adapter = adapter
        binding.rvRequests.layoutManager = LinearLayoutManager(requireContext())
    }

    private fun getCurrentUserId(): String? {
        return FirebaseAuth.getInstance().currentUser?.uid
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}