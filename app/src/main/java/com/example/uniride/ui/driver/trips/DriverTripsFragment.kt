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
                isFull = false
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
                isFull = true
            )
        )

        val allTrips = savedTrips + sampleTrips

        adapter = DriverTripsAdapter(allTrips) { selectedTrip ->
            val bottomSheet = DriverTripDetailBottomSheet(selectedTrip)
            bottomSheet.show(parentFragmentManager, "TravelDetail")
        }

        binding.rvDriverTrips.adapter = adapter
    }

    private fun loadTripsFromSharedPreferences(): List<DriverTripItem> {
        val sharedPreferences = requireActivity().getSharedPreferences("route_data", Context.MODE_PRIVATE)
        val trips = mutableListOf<DriverTripItem>()

        if (sharedPreferences.getBoolean("HAS_PUBLISHED_ROUTE", false)) {
            val origin = sharedPreferences.getString("ROUTE_ORIGIN", "") ?: ""
            val destination = sharedPreferences.getString("ROUTE_DESTINATION", "") ?: ""

            val trip = DriverTripItem(
                travelOption = TravelOption(
                    driverName = "Tú", // Nombre del usuario actual
                    description = "Viaje programado",
                    price = 15000, // Precio por defecto o guardado
                    driverImage = R.drawable.ic_profile,
                    drawableResId = R.drawable.ic_car,
                    availableSeats = 4, // O un valor guardado
                    origin = origin,
                    destination = destination,
                    departureTime = "08:00", // O un valor guardado
                    intermediateStops = getIntermediateStopsFromPrefs(sharedPreferences),
                    travelDate = LocalDate.now() // O la fecha guardada
                ),
                acceptedCount = 0,
                pendingCount = 0,
                hasNewMessages = false,
                isFull = false
            )

            trips.add(trip)
        }

        return trips
    }

    // 2. Método auxiliar para obtener las paradas intermedias
    private fun getIntermediateStopsFromPrefs(sharedPreferences: SharedPreferences): List<String> {
        val stops = mutableListOf<String>()
        val stopsCount = sharedPreferences.getInt("ROUTE_STOPS_COUNT", 0)

        for (i in 0 until stopsCount) {
            val stop = sharedPreferences.getString("ROUTE_STOP_$i", null)
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
