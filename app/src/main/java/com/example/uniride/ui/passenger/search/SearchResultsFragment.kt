package com.example.uniride.ui.passenger.search

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.uniride.R
import com.example.uniride.databinding.FragmentSearchResultsBinding
import com.example.uniride.domain.adapter.TravelOptionAdapter
import com.example.uniride.domain.model.TravelOption
import com.google.android.material.bottomsheet.BottomSheetBehavior
import dagger.hilt.android.AndroidEntryPoint

class SearchResultsFragment : Fragment() {

    private var _binding: FragmentSearchResultsBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: TravelOptionAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSearchResultsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Datos de prueba
        val options = listOf(
            TravelOption(
                driverName = "Conductor 1",
                description = "Rápido y económico",
                origin = "Universidad Nacional",
                destination = "Centro Comercial Santafé",
                departureTime = "4:30 PM",
                price = 11500,
                availableSeats = 2,
                intermediateStops = listOf("UNAL", "Centro", "Santafé"),
                drawableResId = R.drawable.ic_car,
                driverImage = R.drawable.ic_profile
            ),
            TravelOption(
                driverName = "Conductor 2",
                description = "Viaje tranquilo",
                origin = "Universidad Nacional",
                destination = "Titan Plaza",
                departureTime = "4:45 PM",
                price = 9800,
                availableSeats = 3,
                intermediateStops = listOf("UNAL", "Titan"),
                drawableResId = R.drawable.ic_car,
                driverImage = R.drawable.ic_profile
            ),
            TravelOption(
                driverName = "Conductor 2",
                description = "Viaje tranquilo",
                origin = "Universidad Nacional",
                destination = "Titan Plaza",
                departureTime = "4:45 PM",
                price = 9800,
                availableSeats = 3,
                intermediateStops = listOf("UNAL", "Titan"),
                drawableResId = R.drawable.ic_car,
                driverImage = R.drawable.ic_profile
            ),
            TravelOption(
                driverName = "Conductor 2",
                description = "Viaje tranquilo",
                origin = "Universidad Nacional",
                destination = "Titan Plaza",
                departureTime = "4:45 PM",
                price = 9800,
                availableSeats = 3,
                intermediateStops = listOf("UNAL", "Titan"),
                drawableResId = R.drawable.ic_car,
                driverImage = R.drawable.ic_profile
            )
        )

        adapter = TravelOptionAdapter(options) { selected ->
            TravelDetailBottomSheet(selected).show(parentFragmentManager, "TravelDetail")
        }


        binding.rvOptions.layoutManager = LinearLayoutManager(requireContext())
        binding.rvOptions.adapter = adapter

        // Comportamiento del BottomSheet
        val behavior = BottomSheetBehavior.from(binding.bottomSheet)
        binding.bottomSheet.post {
            behavior.peekHeight = 1200
            behavior.state = BottomSheetBehavior.STATE_COLLAPSED
            behavior.isDraggable = true
            behavior.isHideable = false
        }




        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.btnChange.setOnClickListener {
            findNavController().navigate(R.id.searchInputFragment)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}