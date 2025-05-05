package com.example.uniride.ui.passenger.requests

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.uniride.R
import com.example.uniride.databinding.FragmentPassengerRequestsBinding
import com.example.uniride.domain.model.TravelOption
import com.example.uniride.domain.model.TravelRequest
import com.example.uniride.domain.model.TravelRequestStatus
import java.time.LocalDate
import java.time.LocalDateTime

class PassengerRequestsFragment : Fragment() {

    private var _binding: FragmentPassengerRequestsBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: TravelRequestAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPassengerRequestsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val requests = listOf(
            TravelRequest(
                travelOption = TravelOption(
                    driverName = "Juan Pérez",
                    description = "Viaje tranquilo",
                    price = 12000,
                    driverImage = R.drawable.ic_profile,
                    drawableResId = R.drawable.ic_car,
                    availableSeats = 3,
                    origin = "Centro",
                    destination = "Colina",
                    departureTime = "15:30",
                    intermediateStops = listOf("Centro", "Suba", "Colina"),
                    travelDate = LocalDate.parse("2025-05-05")
                ),
                requestDate = LocalDateTime.parse("2025-05-01T10:30:00"),
                status = TravelRequestStatus.Pending
            ),
            TravelRequest(
                travelOption = TravelOption(
                    driverName = "Laura García",
                    description = "Puntual",
                    price = 9800,
                    driverImage = R.drawable.ic_profile,
                    drawableResId = R.drawable.ic_car,
                    availableSeats = 1,
                    origin = "UNAL",
                    destination = "Titan Plaza",
                    departureTime = "16:15",
                    intermediateStops = listOf("UNAL", "Galerías", "Titan"),
                    travelDate = LocalDate.parse("2025-05-06")
                ),
                requestDate = LocalDateTime.parse("2025-05-02T11:45:00"),
                status = TravelRequestStatus.Accepted
            ),
            TravelRequest(
                travelOption = TravelOption(
                    driverName = "Laura García",
                    description = "Puntual",
                    price = 9800,
                    driverImage = R.drawable.ic_profile,
                    drawableResId = R.drawable.ic_car,
                    availableSeats = 1,
                    origin = "UNAL",
                    destination = "Titan Plaza",
                    departureTime = "16:15",
                    intermediateStops = listOf("UNAL", "Galerías", "Titan"),
                    travelDate = LocalDate.parse("2025-05-06")
                ),
                requestDate = LocalDateTime.parse("2025-05-02T11:45:00"),
                status = TravelRequestStatus.Rejected
            ),
            TravelRequest(
                travelOption = TravelOption(
                    driverName = "Carlos Ruiz",
                    description = "Confiable",
                    price = 11000,
                    driverImage = R.drawable.ic_profile,
                    drawableResId = R.drawable.ic_car,
                    availableSeats = 2,
                    origin = "Zona T",
                    destination = "Bulevar",
                    departureTime = "13:50",
                    intermediateStops = listOf("Zona T", "Autonorte", "Bulevar"),
                    travelDate = LocalDate.parse("2025-04-28")
                ),
                requestDate = LocalDateTime.parse("2025-04-25T09:15:00"),
                status = TravelRequestStatus.Finished
            ),
            TravelRequest(
                travelOption = TravelOption(
                    driverName = "Carlos Ruiz",
                    description = "Confiable",
                    price = 11000,
                    driverImage = R.drawable.ic_profile,
                    drawableResId = R.drawable.ic_car,
                    availableSeats = 2,
                    origin = "Zona T",
                    destination = "Bulevar",
                    departureTime = "13:50",
                    intermediateStops = listOf("Zona T", "Autonorte", "Bulevar"),
                    travelDate = LocalDate.parse("2025-04-30")
                ),
                requestDate = LocalDateTime.parse("2025-04-25T09:15:00"),
                status = TravelRequestStatus.Finished
            )
        )

        adapter = TravelRequestAdapter(requests)

        binding.rvRequests.layoutManager = LinearLayoutManager(requireContext())
        binding.rvRequests.adapter = adapter
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}