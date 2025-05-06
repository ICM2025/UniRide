package com.example.uniride

import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.os.Bundle
import android.os.StrictMode
import android.os.StrictMode.ThreadPolicy
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.uniride.databinding.ActivityPassengerHomeBinding
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.location.LocationSettingsResponse
import com.google.android.gms.location.Priority
import com.google.android.gms.location.SettingsClient
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.tasks.Task
import com.google.android.material.bottomsheet.BottomSheetBehavior

class PassengerHomeActivity : BottomMenuActivity(), OnMapReadyCallback {
    private lateinit var binding: ActivityPassengerHomeBinding
    private lateinit var bottomSheetBehavior: BottomSheetBehavior<View>

    // Mapa y ubicación
    private var mMap: GoogleMap? = null
    private lateinit var locationClient: FusedLocationProviderClient
    private lateinit var sensorManager: SensorManager
    private var lightSensor: Sensor? = null
    private lateinit var sensorEventListener: SensorEventListener
    private val defaultLocation = LatLng(4.6097, -74.0817) // Bogotá, Colombia
    private var currentLocation: LatLng? = null
    private var destinationLocation: LatLng? = null

    private val locationPermissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            locationSettings()
        } else {
            Toast.makeText(
                this,
                "Se requieren permisos de ubicación para mostrar la ruta",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private val locationSettings = registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            getCurrentLocation()
        } else {
            Toast.makeText(this, "El GPS está apagado", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPassengerHomeBinding.inflate(layoutInflater)

        // Configurar el contenedor del menú inferior
        bottomMenuBinding.container.removeAllViews()
        bottomMenuBinding.container.addView(binding.root)
        bottomMenuBinding.bottomNav.selectedItemId = R.id.nav_home

        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT)
        sensorEventListener = createSensorEventListener()
        locationClient = LocationServices.getFusedLocationProviderClient(this)

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment?
        mapFragment?.getMapAsync(this)

        val policy = ThreadPolicy.Builder().permitAll().build()
        StrictMode.setThreadPolicy(policy)

        checkLocationPermissions()

        // Configurar el bottom sheet
        setupBottomSheet()

        // Configurar los listeners de los elementos UI
        binding.cardSearch.setOnClickListener { navigateToSearchWheelActivity() }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        mMap?.uiSettings?.isZoomControlsEnabled = true

        if (ActivityCompat.checkSelfPermission(
                this,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(
                this,
                android.Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        mMap?.isMyLocationEnabled = true

        getCurrentLocation()
    }

    private fun checkLocationPermissions() {
        if (ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {

            locationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    updateUI(location)
                    Log.i("LOCATIONKT", "Longitude: ${location.longitude}")
                    Log.i("LOCATIONKT", "Latitude: ${location.latitude}")
                } else {
                    Log.w(
                        "LOCATIONKT",
                        "Ubicación nula. Puede que el GPS esté apagado o no haya señal aún."
                    )
                }
            }
        } else {
            if (shouldShowRequestPermissionRationale(android.Manifest.permission.ACCESS_FINE_LOCATION)) {
                Toast.makeText(
                    this,
                    "El permiso es necesario para acceder a tu ubicación (GPS)",
                    Toast.LENGTH_LONG
                ).show()
            }
            locationPermissionRequest.launch(android.Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    private fun locationSettings() {
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 10000)
            .setWaitForAccurateLocation(true)
            .setMinUpdateIntervalMillis(5000)
            .build()

        val builder = LocationSettingsRequest.Builder().addLocationRequest(locationRequest)
        val client: SettingsClient = LocationServices.getSettingsClient(this)
        val task: Task<LocationSettingsResponse> = client.checkLocationSettings(builder.build())

        task.addOnSuccessListener { locationSettingsResponse ->
            getCurrentLocation()
        }

        task.addOnFailureListener { exception ->
            if (exception is ResolvableApiException) {
                try {
                    // Mostrar diálogo para resolver el problema
                    val isr = IntentSenderRequest.Builder(exception.resolution).build()
                    locationSettings.launch(isr)
                } catch (sendEx: IntentSender.SendIntentException) {
                    Toast.makeText(this, "No hay hardware GPS", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(
                this,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED ||
            ActivityCompat.checkSelfPermission(
                this,
                android.Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {

            mMap?.isMyLocationEnabled = true

            locationClient.lastLocation.addOnSuccessListener { location: Location? ->
                location?.let {
                    currentLocation = LatLng(it.latitude, it.longitude)

                    // Añadir un marcador en la ubicación actual
                    mMap?.let { map ->
                        val markerOptions =
                            MarkerOptions().position(currentLocation!!).title("Mi ubicación")
                                .snippet("Ubicación actual")

                        map.addMarker(markerOptions)
                        map.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLocation!!, 15f))
                    }
                }
            }
        }
    }

    private fun updateUI(location: Location) {
        currentLocation = LatLng(location.latitude, location.longitude)
        Log.i("GPS_APP", "(lat: ${location.latitude}, long: ${location.longitude})")

        mMap?.let { map ->
            map.clear()

            val markerOptions = MarkerOptions()
                .position(currentLocation!!)
                .title("Mi ubicación")
                .snippet(
                    "Lat: ${
                        String.format(
                            "%.6f",
                            location.latitude
                        )
                    }, Lon: ${String.format("%.6f", location.longitude)}"
                )

            map.addMarker(markerOptions)
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLocation!!, 15f))
        }
    }

    private fun createSensorEventListener(): SensorEventListener {
        val sel = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                mMap?.let { googleMap ->
                    if (event != null) {
                        if (event.values[0] > 5000) {
                            mMap?.setMapStyle(
                                MapStyleOptions
                                    .loadRawResourceStyle(this@PassengerHomeActivity, R.raw.lightmap)
                            )
                        } else {
                            mMap?.setMapStyle(
                                MapStyleOptions
                                    .loadRawResourceStyle(this@PassengerHomeActivity, R.raw.darkmap)
                            )
                        }
                    }
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
            }
        }
        return sel
    }


    private fun navigateToSearchWheelActivity() {
        val intent = Intent(baseContext, SearchWheelActivity::class.java)
        startActivity(intent)
    }

    private fun setupBottomSheet() {
        // Inicializa el BottomSheet
        bottomSheetBehavior = BottomSheetBehavior.from(binding.bottomSheet)

        // Comienza colapsado
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED

        // Configurar listeners para los destinos
        binding.layoutDestination1.setOnClickListener {
            // Implementar navegación a destino 1
        }

        binding.layoutDestination2.setOnClickListener {
            // Implementar navegación a destino 2
        }

        binding.cardSearch.setOnClickListener {
            // Expandir el bottom sheet cuando se pulsa la barra de búsqueda
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
            navigateToSearchWheelActivity()
        }
    }

    override fun onResume() {
        super.onResume()
    }
}