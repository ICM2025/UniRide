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
import com.example.uniride.domain.model.PassengerRequestStatus

class TripRequestsFragment : Fragment() {

    private var _binding: FragmentTripRequestBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: PassengerRequestsAdapter

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
        val requests = listOf(
            PassengerRequest(
                passengerName = "Camila Torres",
                destination = "Zona T",
                status = PassengerRequestStatus.Pending,
                profileImg = R.drawable.ic_profile,
                university = "Universidad Nacional",
                email = "camila.torres@email.com",
                tripCount = 12,
                rating = 4.8,
                reviewsCount = 24
            ),
            PassengerRequest(
                passengerName = "Luis Martínez",
                destination = "Suba",
                status = PassengerRequestStatus.Accepted,
                profileImg = R.drawable.ic_profile,
                university = "Universidad de los Andes",
                email = "luis.martinez@email.com",
                tripCount = 18,
                rating = 4.6,
                reviewsCount = 31
            ),
            PassengerRequest(
                passengerName = "Andrea Gómez",
                destination = "Titan Plaza",
                status = PassengerRequestStatus.Rejected,
                profileImg = R.drawable.ic_profile,
                university = "Pontificia Universidad Javeriana",
                email = "andrea.gomez@email.com",
                tripCount = 7,
                rating = 4.9,
                reviewsCount = 15
            )
        )



        adapter = PassengerRequestsAdapter(
            items = requests,
            onAccept = { req ->
                Toast.makeText(requireContext(), "Aceptaste a ${req.passengerName}", Toast.LENGTH_SHORT).show()
            },
            onReject = { req ->
                Toast.makeText(requireContext(), "Rechazaste a ${req.passengerName}", Toast.LENGTH_SHORT).show()
            },
            onClick = { req -> showPassengerDetails(req) }
        )
        // se muestra el bottom sheet
        binding.rvPassengerRequests.adapter = adapter
    }

    private fun showPassengerDetails(request: PassengerRequest) {
        val details = PassengerRequest(
            passengerName = request.passengerName,
            university = request.university,
            email = request.email,
            profileImg = request.profileImg,
            tripCount = request.tripCount,
            rating = request.rating,
            reviewsCount = request.reviewsCount,
            destination = request.destination,
            status = request.status
        )



        PassengerRequestDetailBottomSheet(details) {
            Toast.makeText(requireContext(), "Abrir chat con ${details.passengerName}", Toast.LENGTH_SHORT).show()
            // acá se lanza actividad para el chat
        }.show(parentFragmentManager, "PassengerRequestDetail")
    }



    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
