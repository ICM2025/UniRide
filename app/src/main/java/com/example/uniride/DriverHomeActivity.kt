package com.example.uniride

import Config_permission
import android.content.Intent
import android.location.Location
import android.os.Bundle
import android.widget.Toast
import com.example.uniride.databinding.ActivityDriverHomeBinding
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions

class DriverHomeActivity : BottomMenuActivity(), OnMapReadyCallback {
    private lateinit var binding: ActivityDriverHomeBinding
    private lateinit var locationManager: Config_permission
    private var googleMap: GoogleMap? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDriverHomeBinding.inflate(layoutInflater)
        bottomMenuBinding.container.removeAllViews()
        bottomMenuBinding.container.addView(binding.root)

        // Select specific menu item
        bottomMenuBinding.bottomNav.selectedItemId = R.id.nav_home

        // Initialize map
        setupMap()

        // Initialize location manager
        initializeLocationManager()

        binding.btnRouteInProgress.setOnClickListener {
            val intent = Intent(this, DriverRouteInProgressActivity::class.java).apply {
                putExtra("AVAILABLE_SEATS", 0)
            }
            startActivity(intent)
        }

        binding.btnPublishRoute.setOnClickListener {
            val intent = Intent(this, DriverPublishRouteActivity::class.java)
            startActivity(intent)
        }
    }

    private fun setupMap() {
        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as? SupportMapFragment
        mapFragment?.getMapAsync(this)
    }

    private fun initializeLocationManager() {
        locationManager = Config_permission(this) { location ->
            updateLocationOnMap(location)
        }
        locationManager.initialize()
    }

    private fun updateLocationOnMap(location: Location) {
        val currentLatLng = LatLng(location.latitude, location.longitude)

        googleMap?.let { map ->
            // Clear previous markers
            map.clear()

            // Add marker at current location
            map.addMarker(MarkerOptions()
                .position(currentLatLng)
                .title("Mi ubicación actual"))

            // Move camera to current location
            map.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 15f))
        }
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map

        // Enable location layer if permission is granted
        try {
            map.isMyLocationEnabled = true
        } catch (e: SecurityException) {
            // Will handle permission through locationManager
        }

        // Check location permissions now that map is ready
        locationManager.checkLocationPermissions()
    }

    override fun onResume() {
        super.onResume()
        locationManager.onResume()
    }

    override fun onPause() {
        super.onPause()
        locationManager.onPause()
    }
}