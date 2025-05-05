package com.example.uniride.ui.driver.home

import android.content.Intent
import android.location.Location
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.uniride.R
import com.example.uniride.databinding.FragmentDriverHomeBinding
import com.example.uniride.ui.driver.publish.PublishTripFlowActivity
import com.example.uniride.utility.Config_permission
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment

class DriverHomeFragment : Fragment(), OnMapReadyCallback {

    private var _binding: FragmentDriverHomeBinding? = null
    private val binding get() = _binding!!

    // Location utility
    private lateinit var locationPermissionManager: Config_permission

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDriverHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize permission manager with map callback and location callback
        locationPermissionManager = Config_permission(
            fragment = this,
            mapReadyCallback = { googleMap ->
                // Any additional map customization specific to driver can go here
            },
            locationUpdateCallback = { location ->
                // Any specific driver location update logic can go here
                onLocationUpdated(location)
            }
        )

        // Initialize the location manager
        locationPermissionManager.initialize()

        // Setup map fragment
        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment?
        mapFragment?.getMapAsync(this)

        // Set up button click listener
        binding.btnPublishTrip.setOnClickListener {
            val intent = Intent(requireContext(), PublishTripFlowActivity::class.java)
            startActivity(intent)
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        // Delegate to the location permission manager
        locationPermissionManager.onMapReady(googleMap)
    }

    // Handle specific driver actions when location is updated
    private fun onLocationUpdated(location: Location) {
        // Any additional logic specific to driver view when location changes
        // For example, update nearest pickup points, etc.
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