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

        // Initialize permission manager with map callback and location callback
        locationPermissionManager = Config_permission(
            fragment = this,
            mapReadyCallback = { googleMap ->
                // Any additional map customization specific to passenger can go here
            },
            locationUpdateCallback = { location ->
                // Any specific passenger location update logic can go here
                onLocationUpdated(location)
            }
        )

        // Initialize the location manager
        locationPermissionManager.initialize()

        // Setup map fragment
        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment?
        mapFragment?.getMapAsync(this)

        // Setup bottom sheet
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
        // Delegate to the location permission manager
        locationPermissionManager.onMapReady(googleMap)
    }

    // Handle specific passenger actions when location is updated
    private fun onLocationUpdated(location: Location) {
        // Set up destination search click listener here since it depends on location
        binding.etDestination.setOnClickListener {
            val intent = Intent(requireContext(), SearchFlowActivity::class.java)
            startActivity(intent)
        }

        // Any additional logic specific to passenger view when location changes
        // For example, show nearby drivers, update ETA, etc.
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