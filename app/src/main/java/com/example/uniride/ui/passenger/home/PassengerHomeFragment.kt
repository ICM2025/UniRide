package com.example.uniride.ui.passenger.home

import android.content.Intent
import android.location.Location
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.uniride.R
import com.example.uniride.databinding.FragmentPassengerHomeBinding
import com.example.uniride.ui.passenger.search.SearchFlowActivity
import com.example.uniride.utility.Config_permission
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.material.bottomsheet.BottomSheetBehavior

class PassengerHomeFragment : Fragment(), OnMapReadyCallback {

    private var _binding: FragmentPassengerHomeBinding? = null
    private val binding get() = _binding!!

    private lateinit var bottomSheetBehavior: BottomSheetBehavior<View>

    // Location utility
    private lateinit var locationPermissionManager: Config_permission

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPassengerHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        locationPermissionManager = Config_permission(
            fragment = this,
            mapReadyCallback = { googleMap ->
            },
            locationUpdateCallback = { location ->
                onLocationUpdated(location)
            }
        )

        locationPermissionManager.initialize()

        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment?
        mapFragment?.getMapAsync(this)

        setupBottomSheet()
    }

    private fun setupBottomSheet() {
        bottomSheetBehavior = BottomSheetBehavior.from(binding.bottomSheet)

        binding.bottomSheet.post {
            bottomSheetBehavior.peekHeight = 600
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
            bottomSheetBehavior.isDraggable = true
            bottomSheetBehavior.isHideable = false
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        locationPermissionManager.onMapReady(googleMap)
    }

    private fun onLocationUpdated(location: Location) {
        binding.etDestination.setOnClickListener {
            val intent = Intent(requireContext(), SearchFlowActivity::class.java)
            startActivity(intent)
        }
    }

    override fun onResume() {
        super.onResume()
        locationPermissionManager.onResume()
    }

    override fun onPause() {
        super.onPause()
        locationPermissionManager.onPause()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}