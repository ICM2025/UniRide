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
                                    acceptedCount = 1,
                                    pendingCount = 2,
                                    isFull = false,
                                    hasNewMessages = false,
                                    tripId = trip.id ?: "" // Usar el ID del documento de Firebase
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


//    private fun loadTrips() {
//        val savedTrips = loadTripsFromSharedPreferences()
//        val sampleTrips = listOf(
//            DriverTripItem(
//                travelOption = TravelOption(
//                    driverName = "Juan Pérez",
//                    description = "Viaje tranquilo",
//                    price = 12000,
//                    driverImage = R.drawable.ic_profile,
//                    drawableResId = R.drawable.ic_car,
//                    availableSeats = 5,
//                    origin = "UNAL",
//                    destination = "Santafé",
//                    departureTime = "08:00",
//                    intermediateStops = listOf("Centro", "Suba"),
//                    travelDate = LocalDate.now()
//                ),
//                acceptedCount = 4,
//                pendingCount = 2,
//                hasNewMessages = true,
//                isFull = false,
//                tripId = "sample_trip_1"
//            ),
//            DriverTripItem(
//                travelOption = TravelOption(
//                    driverName = "Juan Pérez",
//                    description = "Viaje tranquilo",
//                    price = 11500,
//                    driverImage = R.drawable.ic_profile,
//                    drawableResId = R.drawable.ic_car,
//                    availableSeats = 3,
//                    origin = "Zona T",
//                    destination = "Portal Norte",
//                    departureTime = "17:30",
//                    intermediateStops = listOf(),
//                    travelDate = LocalDate.now().plusDays(1)
//                ),
//                acceptedCount = 3,
//                pendingCount = 0,
//                hasNewMessages = false,
//                isFull = true,
//                tripId = "sample_trip_2"
//            )
//        )

//        val allTrips = savedTrips + sampleTrips
//
//        adapter = DriverTripsAdapter(allTrips) { selectedTrip ->
//            val bottomSheet = DriverTripDetailBottomSheet(selectedTrip) {
//                // Callback para actualizar la lista cuando se modifique un viaje
//                loadTrips()
//            }
//            bottomSheet.show(parentFragmentManager, "TravelDetail")
//        }
//
//        binding.rvDriverTrips.adapter = adapter
//
//        // Mostrar mensaje si no hay viajes
//        if (allTrips.isEmpty()) {
//            // Aquí puedes mostrar un mensaje de "no hay viajes" si lo deseas
//        }
    //}

//    private fun loadTripsFromSharedPreferences(): List<DriverTripItem> {
//        val sharedPreferences = requireActivity().getSharedPreferences("route_data", Context.MODE_PRIVATE)
//        val trips = mutableListOf<DriverTripItem>()
//
//        // Obtener todos los IDs de viajes guardados
//        val tripIds = sharedPreferences.getStringSet("SAVED_TRIP_IDS", emptySet()) ?: emptySet()
//
//        for (tripId in tripIds) {
//            val origin = sharedPreferences.getString("TRIP_${tripId}_ORIGIN", "") ?: ""
//            val destination = sharedPreferences.getString("TRIP_${tripId}_DESTINATION", "") ?: ""
//            val date = sharedPreferences.getString("TRIP_${tripId}_DATE", "") ?: ""
//            val time = sharedPreferences.getString("TRIP_${tripId}_TIME", "") ?: ""
//
//            if (origin.isNotEmpty() && destination.isNotEmpty()) {
//                val intermediateStops = getIntermediateStopsFromPrefs(sharedPreferences, tripId)
//                val isActiveTrip = sharedPreferences.getString("ACTIVE_TRIP_ID", "") == tripId
//
//                // Convertir fecha si está disponible
//                val travelDate = if (date.isNotEmpty()) {
//                    try {
//                        val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.getDefault())
//                        LocalDate.parse(date, formatter)
//                    } catch (e: Exception) {
//                        LocalDate.now()
//                    }
//                } else {
//                    LocalDate.now()
//                }
//
//                // Usar tiempo guardado o tiempo por defecto
//                val departureTime = if (time.isNotEmpty()) time else "08:00"
//
//                val trip = DriverTripItem(
//                    travelOption = TravelOption(
//                        driverName = "Tú",
//                        description = if (isActiveTrip) "Viaje en curso" else "Viaje programado",
//                        price = 15000,
//                        driverImage = R.drawable.ic_profile,
//                        drawableResId = R.drawable.ic_car,
//                        availableSeats = 4,
//                        origin = origin,
//                        destination = destination,
//                        departureTime = departureTime,
//                        intermediateStops = intermediateStops,
//                        travelDate = travelDate
//                    ),
//                    acceptedCount = if (isActiveTrip) 2 else 0, // Ejemplo de datos
//                    pendingCount = if (isActiveTrip) 1 else 0,
//                    hasNewMessages = isActiveTrip,
//                    isFull = false,
//                    tripId = tripId
//                )
//
//                trips.add(trip)
//            }
//        }
//
//        return trips
//    }
//
//    fun reloadTrips() {
//        loadTrips()
//    }

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