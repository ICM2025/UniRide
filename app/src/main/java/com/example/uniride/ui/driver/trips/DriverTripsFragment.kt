package com.example.uniride.ui.driver.trips

import android.content.Context
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
import com.example.uniride.domain.model.DriverTripItem
import com.example.uniride.domain.model.TravelOption
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.text.SimpleDateFormat
import java.util.*

class DriverTripsFragment : Fragment() {

    private var _binding: FragmentDriverTripsBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: DriverTripsAdapter

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
        loadTrips()
    }

    override fun onResume() {
        super.onResume()
        loadTrips()
    }

    private fun setupRecyclerView() {
        binding.rvDriverTrips.layoutManager = LinearLayoutManager(requireContext())
    }

    private fun loadTrips() {
        val savedTrips = loadTripsFromSharedPreferences()
        val sampleTrips = listOf(
            DriverTripItem(
                travelOption = TravelOption(
                    driverName = "Juan Pérez",
                    description = "Viaje tranquilo",
                    price = 12000,
                    driverImage = R.drawable.ic_profile,
                    drawableResId = R.drawable.ic_car,
                    availableSeats = 5,
                    origin = "UNAL",
                    destination = "Santafé",
                    departureTime = "08:00",
                    intermediateStops = listOf("Centro", "Suba"),
                    travelDate = LocalDate.now()
                ),
                acceptedCount = 4,
                pendingCount = 2,
                hasNewMessages = true,
                isFull = false,
                tripId = "sample_trip_1"
            ),
            DriverTripItem(
                travelOption = TravelOption(
                    driverName = "Juan Pérez",
                    description = "Viaje tranquilo",
                    price = 11500,
                    driverImage = R.drawable.ic_profile,
                    drawableResId = R.drawable.ic_car,
                    availableSeats = 3,
                    origin = "Zona T",
                    destination = "Portal Norte",
                    departureTime = "17:30",
                    intermediateStops = listOf(),
                    travelDate = LocalDate.now().plusDays(1)
                ),
                acceptedCount = 3,
                pendingCount = 0,
                hasNewMessages = false,
                isFull = true,
                tripId = "sample_trip_2"
            )
        )

        val allTrips = savedTrips + sampleTrips

        adapter = DriverTripsAdapter(allTrips) { selectedTrip ->
            val bottomSheet = DriverTripDetailBottomSheet(selectedTrip) {
                // Callback para actualizar la lista cuando se modifique un viaje
                loadTrips()
            }
            bottomSheet.show(parentFragmentManager, "TravelDetail")
        }

        binding.rvDriverTrips.adapter = adapter

        // Mostrar mensaje si no hay viajes
        if (allTrips.isEmpty()) {
            // Aquí puedes mostrar un mensaje de "no hay viajes" si lo deseas
        }
    }

    private fun loadTripsFromSharedPreferences(): List<DriverTripItem> {
        val sharedPreferences = requireActivity().getSharedPreferences("route_data", Context.MODE_PRIVATE)
        val trips = mutableListOf<DriverTripItem>()

        // Obtener todos los IDs de viajes guardados
        val tripIds = sharedPreferences.getStringSet("SAVED_TRIP_IDS", emptySet()) ?: emptySet()

        for (tripId in tripIds) {
            val origin = sharedPreferences.getString("TRIP_${tripId}_ORIGIN", "") ?: ""
            val destination = sharedPreferences.getString("TRIP_${tripId}_DESTINATION", "") ?: ""
            val date = sharedPreferences.getString("TRIP_${tripId}_DATE", "") ?: ""
            val time = sharedPreferences.getString("TRIP_${tripId}_TIME", "") ?: ""

            if (origin.isNotEmpty() && destination.isNotEmpty()) {
                val intermediateStops = getIntermediateStopsFromPrefs(sharedPreferences, tripId)
                val isActiveTrip = sharedPreferences.getString("ACTIVE_TRIP_ID", "") == tripId

                // Convertir fecha si está disponible
                val travelDate = if (date.isNotEmpty()) {
                    try {
                        val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.getDefault())
                        LocalDate.parse(date, formatter)
                    } catch (e: Exception) {
                        LocalDate.now()
                    }
                } else {
                    LocalDate.now()
                }

                // Usar tiempo guardado o tiempo por defecto
                val departureTime = if (time.isNotEmpty()) time else "08:00"

                val trip = DriverTripItem(
                    travelOption = TravelOption(
                        driverName = "Tú",
                        description = if (isActiveTrip) "Viaje en curso" else "Viaje programado",
                        price = 15000,
                        driverImage = R.drawable.ic_profile,
                        drawableResId = R.drawable.ic_car,
                        availableSeats = 4,
                        origin = origin,
                        destination = destination,
                        departureTime = departureTime,
                        intermediateStops = intermediateStops,
                        travelDate = travelDate
                    ),
                    acceptedCount = if (isActiveTrip) 2 else 0, // Ejemplo de datos
                    pendingCount = if (isActiveTrip) 1 else 0,
                    hasNewMessages = isActiveTrip,
                    isFull = false,
                    tripId = tripId
                )

                trips.add(trip)
            }
        }

        return trips
    }

    fun reloadTrips() {
        loadTrips()
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