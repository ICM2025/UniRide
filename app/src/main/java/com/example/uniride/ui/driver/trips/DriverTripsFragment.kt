package com.example.uniride.ui.driver.trips

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

        adapter = DriverTripsAdapter(sampleTrips) { selectedTrip ->
            val bottomSheet = DriverTripDetailBottomSheet(selectedTrip)
            bottomSheet.show(parentFragmentManager, "TravelDetail")
        }

        binding.rvDriverTrips.adapter = adapter
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
