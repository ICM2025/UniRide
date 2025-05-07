package com.example.uniride.utility

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.IntentSender
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.uniride.R
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.location.LocationSettingsResponse
import com.google.android.gms.location.Priority
import com.google.android.gms.location.SettingsClient
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.tasks.Task

class Config_permission(
    private val fragment: Fragment,
    private val mapReadyCallback: (GoogleMap) -> Unit,
    private val locationUpdateCallback: ((Location) -> Unit)? = null
) {
    // Constants
    private val TAG = "LocationPermission"
    private val defaultLocation = LatLng(4.6097, -74.0817) // Bogotá, Colombia

    // Location components
    private lateinit var locationClient: FusedLocationProviderClient
    private lateinit var locationRequest: LocationRequest
    private lateinit var locationCallback: LocationCallback
    private var lastKnownLocation: Location? = null
    private var mMap: GoogleMap? = null

    // Sensor components
    private lateinit var sensorManager: SensorManager
    private var lightSensor: Sensor? = null
    private lateinit var sensorEventListener: SensorEventListener

    // Permission launchers
    private lateinit var locationPermissionLauncher: ActivityResultLauncher<String>
    private lateinit var locationSettingsLauncher: ActivityResultLauncher<IntentSenderRequest>

    fun initialize() {
        initializeLocationComponents()
        initializeSensorComponents()
        registerPermissionLaunchers()
    }

    fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        mMap?.uiSettings?.isZoomControlsEnabled = true

        applyMapSettings()

        mapReadyCallback(googleMap)

        checkLocationPermissions()
    }

    private fun initializeLocationComponents() {
        locationClient = LocationServices.getFusedLocationProviderClient(fragment.requireActivity())
        locationRequest = createLocationRequest()
        locationCallback = createLocationCallback()
    }

    private fun initializeSensorComponents() {
        sensorManager = fragment.requireActivity().getSystemService(Context.SENSOR_SERVICE) as SensorManager
        lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT)
        sensorEventListener = createSensorEventListener()
    }

    private fun registerPermissionLaunchers() {
        locationPermissionLauncher = fragment.registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted ->
            if (isGranted) {
                checkLocationSettings()
            } else {
                Toast.makeText(fragment.requireContext(),"Se requieren permisos de ubicación para mostrar la ruta",Toast.LENGTH_SHORT).show()
            }
        }

        locationSettingsLauncher = fragment.registerForActivityResult(
            ActivityResultContracts.StartIntentSenderForResult()
        ) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                getCurrentLocation()
                startLocationUpdates()
            } else {
                Toast.makeText(fragment.requireContext(), "El GPS está apagado", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun applyMapSettings() {
        updateMapStyle(5000f)
    }

    fun checkLocationPermissions() {
        if (hasLocationPermissions()) {
            requestLastLocation()
            checkLocationSettings()
        } else {
            if (ActivityCompat.shouldShowRequestPermissionRationale(
                    fragment.requireActivity(),
                    Manifest.permission.ACCESS_FINE_LOCATION
                )
            ) {
                Toast.makeText(fragment.requireContext(),"El permiso es necesario para acceder a tu ubicación (GPS)",Toast.LENGTH_LONG).show()
            }
            locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    private fun hasLocationPermissions(): Boolean {
        return ContextCompat.checkSelfPermission(
            fragment.requireContext(),
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(
                    fragment.requireContext(),
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestLastLocation() {
        if (hasLocationPermissions()) {
            try {
                locationClient.lastLocation.addOnSuccessListener { location ->
                    if (location != null) {
                        lastKnownLocation = location
                        updateUI(location)
                        Log.i(TAG, "Longitude: ${location.longitude}")
                        Log.i(TAG, "Latitude: ${location.latitude}")
                    } else {
                        Log.w(TAG, "Ubicación nula. Puede que el GPS esté apagado o no haya señal aún.")
                        checkLocationSettings()
                    }
                }
            } catch (e: SecurityException) {
                Log.e(TAG, "Error de permisos al obtener la ubicación: ${e.message}")
            }
        }
    }

    private fun createLocationRequest(): LocationRequest {
        return LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 10000)
            .setWaitForAccurateLocation(true)
            .setMinUpdateIntervalMillis(5000)
            .build()
    }

    private fun createLocationCallback(): LocationCallback {
        return object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                for (location in result.locations) {
                    updateUI(location)
                }
            }
        }
    }

    private fun createSensorEventListener(): SensorEventListener {
        return object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                if (event != null) {
                    updateMapStyle(event.values[0])
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
            }
        }
    }

    private fun updateMapStyle(lightValue: Float) {
        mMap?.let { googleMap ->
            if (lightValue > 5000) {
                googleMap.setMapStyle(
                    MapStyleOptions.loadRawResourceStyle(fragment.requireContext(), R.raw.lightmap)
                )
            } else {
                googleMap.setMapStyle(
                    MapStyleOptions.loadRawResourceStyle(fragment.requireContext(), R.raw.darkmap)
                )
            }
        }
    }

    private fun checkLocationSettings() {
        val builder = LocationSettingsRequest.Builder().addLocationRequest(locationRequest)
        val client: SettingsClient = LocationServices.getSettingsClient(fragment.requireActivity())
        val task: Task<LocationSettingsResponse> = client.checkLocationSettings(builder.build())

        task.addOnSuccessListener { locationSettingsResponse ->
            getCurrentLocation()
            startLocationUpdates()
        }

        task.addOnFailureListener { exception ->
            if (exception is ResolvableApiException) {
                try {
                    val isr = IntentSenderRequest.Builder(exception.resolution).build()
                    locationSettingsLauncher.launch(isr)
                } catch (sendEx: IntentSender.SendIntentException) {
                    Toast.makeText(fragment.requireContext(), "No hay hardware GPS", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun getCurrentLocation() {
        if (hasLocationPermissions()) {
            mMap?.isMyLocationEnabled = true

            locationClient.lastLocation.addOnSuccessListener { location: Location? ->
                location?.let {
                    updateUI(it)
                } ?: run {
                    mMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLocation, 15f))
                }
            }
        }
    }

    private fun updateUI(location: Location) {
        val currentLocation = LatLng(location.latitude, location.longitude)
        Log.i("GPS_APP", "(lat: ${location.latitude}, long: ${location.longitude})")

        mMap?.let { map ->
            map.clear()

            val markerOptions = MarkerOptions()
                .position(currentLocation)
                .title("Mi ubicación")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))
                .snippet(
                    "Lat: ${
                        String.format(
                            "%.6f",
                            location.latitude
                        )
                    }, Lon: ${String.format("%.6f", location.longitude)}"
                )

            map.addMarker(markerOptions)
            map.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLocation, 15f))
        }
        locationUpdateCallback?.invoke(location)
    }

    @SuppressLint("MissingPermission")
    fun startLocationUpdates() {
        if (hasLocationPermissions()) {
            locationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )
        }
    }

    fun stopLocationUpdates() {
        locationClient.removeLocationUpdates(locationCallback)
    }

    fun onResume() {
        lightSensor?.let {
            sensorManager.registerListener(
                sensorEventListener,
                it,
                SensorManager.SENSOR_DELAY_NORMAL
            )
        }

        if (hasLocationPermissions()) {
            startLocationUpdates()
        } else {
            checkLocationPermissions()
        }
    }

    fun onPause() {
        sensorManager.unregisterListener(sensorEventListener)
        stopLocationUpdates()
    }

    fun getMap(): GoogleMap? {
        return mMap
    }

    fun getLastKnownLocation(): Location? {
        return lastKnownLocation
    }
}