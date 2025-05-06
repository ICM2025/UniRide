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
                profileDrawableRes = R.id.iv_profile
            ),
            PassengerRequest(
                passengerName = "Luis Martínez",
                destination = "Suba",
                status = PassengerRequestStatus.Accepted,
                profileDrawableRes = R.id.iv_profile
            ),
            PassengerRequest(
                passengerName = "Andrea Gómez",
                destination = "Titan Plaza",
                status = PassengerRequestStatus.Rejected,
                profileDrawableRes = R.id.iv_profile
            )
        )

        adapter = PassengerRequestsAdapter(
            items = requests,
            onAccept = { req ->
                Toast.makeText(requireContext(), "Aceptaste a ${req.passengerName}", Toast.LENGTH_SHORT).show()
                // Lógica real para aceptar
            },
            onReject = { req ->
                Toast.makeText(requireContext(), "Rechazaste a ${req.passengerName}", Toast.LENGTH_SHORT).show()
                // Lógica real para rechazar
            }
        )

        binding.rvPassengerRequests.adapter = adapter
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
