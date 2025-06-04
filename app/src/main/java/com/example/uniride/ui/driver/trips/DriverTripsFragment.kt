package com.example.uniride.ui.driver.trips

import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.uniride.R
import com.example.uniride.databinding.FragmentDriverTripsBinding
import com.example.uniride.domain.adapter.DriverTripsAdapter
import com.example.uniride.domain.model.Car
import com.example.uniride.domain.model.DriverTripItem
import com.example.uniride.domain.model.Route
import com.example.uniride.domain.model.Trip
import com.example.uniride.domain.model.front.TripInformation
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.time.LocalDate
import java.util.Arrays

class DriverTripsFragment : Fragment() {

    private var _binding: FragmentDriverTripsBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: DriverTripsAdapter

    private val db = FirebaseFirestore.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDriverTripsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        fetchDriverTrips()
    }

    override fun onResume() {
        super.onResume()
        fetchDriverTrips()
    }

    private fun setupRecyclerView() {
        binding.rvDriverTrips.layoutManager = LinearLayoutManager(requireContext())
    }

    //Trae todos los viajes del conductor
    private fun fetchDriverTrips() {
        val driverId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        db.collection("Trips")
            .whereEqualTo("idDriver", driverId)
            .whereIn("status", listOf("ACTIVE", "PENDING"))
            .get()
            .addOnSuccessListener { snapshot ->
                val trips = snapshot.documents.mapNotNull { it.toObject(Trip::class.java) }
                if (trips.isEmpty()) {
                    showEmptyState()
                } else {
                    buildDriverTripItems(trips)
                }
            }
    }

    private fun buildDriverTripItems(trips: List<Trip>) {
        val items = mutableListOf<DriverTripItem>()
        var completedCount = 0

        for (trip in trips) {
            val tripId = trip.id ?: continue

            db.collection("PassengerRequests")
                .whereEqualTo("idTrip", tripId)
                .get()
                .addOnSuccessListener { reqSnap ->
                    val acceptedCount = reqSnap.documents.count { it.getString("status") == "ACCEPTED" }
                    val pendingCount = reqSnap.documents.count { it.getString("status") == "PENDING" }

                    val isFull = acceptedCount >= trip.places + acceptedCount

                    db.collection("Routes").document(trip.idRoute)
                        .get()
                        .addOnSuccessListener { routeSnap ->
                            val route = routeSnap.toObject(Route::class.java) ?: return@addOnSuccessListener

                            fetchStopNames(route.idOrigin, route.idDestination) { origin, destination ->
                                db.collection("Cars").document(trip.idCar)
                                    .get()
                                    .addOnSuccessListener { carSnap ->
                                        val car = carSnap.toObject(Car::class.java)

                                        val parsedDate = try {
                                            LocalDate.parse(trip.date)
                                        } catch (e: Exception) {
                                            LocalDate.now()
                                        }

                                        val tripInfo = TripInformation(
                                            carIcon = R.drawable.ic_car,
                                            availableSeats = trip.places,
                                            origin = origin,
                                            destination = destination,
                                            departureTime = trip.startTime,
                                            travelDate = parsedDate,
                                            price = trip.price,
                                            carName = "${car?.brand} ${car?.model}",
                                            carPlate = car?.licensePlate ?: "Sin placa"
                                        )

                                        val item = DriverTripItem(
                                            tripInformation = tripInfo,
                                            acceptedCount = acceptedCount,
                                            pendingCount = pendingCount,
                                            isFull = isFull,
                                            hasNewMessages = false,
                                            tripId = tripId
                                        )

                                        items.add(item)
                                        completedCount++
                                        if (completedCount == trips.size) {
                                            showTrips(items)
                                        }
                                    }
                                    .addOnFailureListener {
                                        completedCount++
                                        if (completedCount == trips.size) {
                                            showTrips(items)
                                        }
                                    }
                            }
                        }
                        .addOnFailureListener {
                            completedCount++
                            if (completedCount == trips.size) {
                                showTrips(items)
                            }
                        }
                }
                .addOnFailureListener {
                    completedCount++
                    if (completedCount == trips.size) {
                        showTrips(items)
                    }
                }
        }
    }



    //convierte id de origen y destino a sus nombres correspondientes
    private fun fetchStopNames(originId: String, destinationId: String, onResult: (String, String) -> Unit) {
        db.collection("Stops").document(originId).get().addOnSuccessListener { originSnap ->
            val originName = originSnap.getString("name") ?: "Origen"

            db.collection("Stops").document(destinationId).get().addOnSuccessListener { destSnap ->
                val destName = destSnap.getString("name") ?: "Destino"
                onResult(originName, destName)
            }
        }
    }
    //muestra los viajes gracias al adapter
    private fun showTrips(items: List<DriverTripItem>) {
        adapter = DriverTripsAdapter(items) { selectedTrip ->
            val bottomSheet = DriverTripDetailBottomSheet(selectedTrip) {
                fetchDriverTrips()
            }
            bottomSheet.show(parentFragmentManager, "TravelDetail")
        }
        binding.rvDriverTrips.adapter = adapter
    }

    private fun showEmptyState() {
        // luego acá se cambia por si no hay ninguno
    }

    private fun getIntermediateStopsFromPrefs(sharedPreferences: SharedPreferences, tripId: String): List<String> {
        val stops = mutableListOf<String>()
        val stopsCount = sharedPreferences.getInt("TRIP_${tripId}_STOPS_COUNT", 0)

        for (i in 0 until stopsCount) {
            val stop = sharedPreferences.getString("TRIP_${tripId}_STOP_$i", null)
            if (!stop.isNullOrEmpty()) {
                stops.add(stop)
            }
        }

        return stops
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}