package com.example.uniride.ui.driver.home

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Color
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.StrictMode
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.uniride.R
import com.example.uniride.databinding.FragmentDriverHomeBinding
import com.example.uniride.ui.MainActivity
import com.example.uniride.ui.driver.publish.PublishTripFlowActivity
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.Polyline
import com.google.android.gms.maps.model.PolylineOptions
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors

class DriverHomeFragment : Fragment(), OnMapReadyCallback, LocationListener {

    private var _binding: FragmentDriverHomeBinding? = null
    private val binding get() = _binding!!

    // Location components
    private lateinit var locationManager: LocationManager
    private val locationPermissionCode = 1001
    private var hasLocationPermission = false

    private lateinit var sensorManager: SensorManager
    private var lightSensor: Sensor? = null
    private lateinit var sensorEventListener: SensorEventListener

    // Route components
    private var mMap: GoogleMap? = null
    private var routePolyline: Polyline? = null
    private lateinit var geocoder: Geocoder
    private lateinit var sharedPreferences: SharedPreferences
    private val defaultLocation = LatLng(4.6097, -74.0817) // Bogotá, Colombia

    // Location variables
    private var originLocation: LatLng? = null
    private var destinationLocation: LatLng? = null
    private var stopLocations: MutableList<LatLng> = mutableListOf()
    private var currentRoutePolyline: Polyline? = null
    private var currentLocation: Location? = null

    private lateinit var publishTripLauncher: ActivityResultLauncher<Intent>

    private var isRouteDisplayed = false
    private var preventLocationCameraUpdate = false
    private var isInitialLocationSet = false

    // Permission launcher
    private lateinit var requestPermissionLauncher: ActivityResultLauncher<String>

    // Thread executor for network operations
    private val networkExecutor = Executors.newSingleThreadExecutor()

    // API Key
    private val DIRECTIONS_API_KEY: String by lazy {
        val applicationInfo = requireActivity().packageManager.getApplicationInfo(
            requireActivity().packageName,
            PackageManager.GET_META_DATA
        )
        applicationInfo.metaData.getString("com.google.android.geo.API_KEY") ?: ""
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDriverHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // inicialmente se oculta todo para primero verificar si está registrado como conductor
        binding.btnPublishTrip.visibility = View.GONE
        val mapFragment = childFragmentManager.findFragmentById(R.id.map)
        mapFragment?.view?.visibility = View.GONE
        val promptContainer = binding.root.findViewById<View>(R.id.registerPromptContainer)
        promptContainer.visibility = View.GONE

        // oculta el menú de navegación inferior
        requireActivity().findViewById<BottomNavigationView>(R.id.bottom_nav)
            ?.visibility = View.GONE

        // oculta el botón del menú lateral
        requireActivity().findViewById<ImageButton>(R.id.btn_menu)?.visibility = View.GONE




        // verificación de si es conductor
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val db = FirebaseFirestore.getInstance()

        db.collection("drivers").document(uid).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    // si es conductor se muestran los componentes de home
                    binding.btnPublishTrip.visibility = View.VISIBLE
                    mapFragment?.view?.visibility = View.VISIBLE

                    // mostrar el menú inferior
                    requireActivity().findViewById<BottomNavigationView>(R.id.bottom_nav)
                        ?.visibility = View.VISIBLE

                    // mostrar botón del menú lateral
                    requireActivity().findViewById<ImageButton>(R.id.btn_menu)
                        ?.visibility = View.VISIBLE


                } else {
                    // si no es se muestra el layout de registro
                    promptContainer.visibility = View.VISIBLE
                    //navegar a registrarse como conductor
                    val btnRegisterDriver = promptContainer.findViewById<Button>(R.id.btnGoToRegisterDriver)
                    btnRegisterDriver.setOnClickListener {
                        findNavController().navigate(R.id.action_driverHomeFragment_to_registerDriverFragment)
                    }

                    //volver al menú de pasajero
                    val btnReturn = promptContainer.findViewById<Button>(R.id.btnReturnToPassenger)
                    btnReturn.setOnClickListener {
                        requireActivity().findViewById<BottomNavigationView>(R.id.bottom_nav)
                            ?.visibility = View.VISIBLE

                        // mostrar botón del menú lateral
                        requireActivity().findViewById<ImageButton>(R.id.btn_menu)
                            ?.visibility = View.VISIBLE
                        (activity as? MainActivity)?.switchToPassengerMode()
                    }
                }
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Error al verificar rol de conductor",
                    Toast.LENGTH_SHORT).show()
            }


        //flujo para mapa, ubicación y permisos
        sharedPreferences = requireActivity().getSharedPreferences("route_data", Context.MODE_PRIVATE)
        geocoder = Geocoder(requireContext())
        registerPublishTripLauncher()

        val policy = StrictMode.ThreadPolicy.Builder().permitAll().build()
        StrictMode.setThreadPolicy(policy)

        locationManager = requireActivity().getSystemService(Context.LOCATION_SERVICE) as LocationManager

        setupPermissionLauncher()
        isInitialLocationSet = false
        preventLocationCameraUpdate = false
        initializeSensorComponents()

        val supportMapFragment = mapFragment as? SupportMapFragment
        supportMapFragment?.getMapAsync(this)

        binding.btnPublishTrip.setOnClickListener {
            val intent = Intent(requireContext(), PublishTripFlowActivity::class.java)
            publishTripLauncher.launch(intent)
        }

        checkLocationPermission()


    }


    private fun setupPermissionLauncher() {
        requestPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted ->
            if (isGranted) {
                // Permission granted
                hasLocationPermission = true
                startLocationUpdates()
                enableMyLocation()
            } else {
                // Permission denied
                Toast.makeText(requireContext(),"La ubicación es necesaria para mostrar tu posición en el mapa",Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            hasLocationPermission = true
            startLocationUpdates()
        } else {
            requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    private fun initializeSensorComponents() {
        sensorManager = requireActivity().getSystemService(Context.SENSOR_SERVICE) as SensorManager
        lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT)
        sensorEventListener = createSensorEventListener()
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
                    MapStyleOptions.loadRawResourceStyle(requireContext(), R.raw.lightmap)
                )
            } else {
                googleMap.setMapStyle(
                    MapStyleOptions.loadRawResourceStyle(requireContext(), R.raw.darkmap)
                )
            }
        }
    }

    private fun startLocationUpdates() {
        if (hasLocationPermission) {
            try {
                if (ActivityCompat.checkSelfPermission(
                        requireContext(),
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    locationManager.requestLocationUpdates(
                        LocationManager.GPS_PROVIDER,
                        5000,
                        10f,
                        this
                    )

                    val lastLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                    if (lastLocation != null) {
                        onLocationChanged(lastLocation)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(requireContext(),"Error al iniciar actualizaciones de ubicación: ${e.message}",Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun enableMyLocation() {
        if (mMap != null && hasLocationPermission) {
            if (ActivityCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                mMap?.isMyLocationEnabled = true
                getCurrentLocationAndCenter()
            }
        }
    }

    private fun registerPublishTripLauncher() {
        publishTripLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == android.app.Activity.RESULT_OK) {
            }
        }}

    private fun updateUIForActiveRoute() {
        if (sharedPreferences.getBoolean("HAS_PUBLISHED_ROUTE", false)) {
            binding.btnPublishTrip.text = "Editar Viaje"
            preventLocationCameraUpdate = true
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        googleMap.uiSettings.apply {
            isZoomControlsEnabled = true
            isMapToolbarEnabled = true
            isZoomGesturesEnabled = true
            isScrollGesturesEnabled = true
            isRotateGesturesEnabled = true
        }
        mMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLocation, 15f))


        lightSensor?.let {
            val currentLightValue = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT)?.let { 10000f } ?: 100f
            updateMapStyle(currentLightValue)
        }

        enableMyLocation()
        if (hasLocationPermission) {
            getCurrentLocationAndCenter()
        } else {
            // Si no hay permisos, usar ubicación por defecto
            mMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLocation, 15f))
        }
        updateUIForActiveRoute()

        if (sharedPreferences.getBoolean("HAS_ACTIVE_ROUTE", false)) {
            loadRouteAndDisplay()
        }
    }

    private fun loadRouteAndDisplay() {
        // Solo cargar la ruta si hay una ruta activa, no solo publicada
        if (sharedPreferences.getBoolean("HAS_ACTIVE_ROUTE", false)) {
            val originAddress = sharedPreferences.getString("ROUTE_ORIGIN", null)
            val destinationAddress = sharedPreferences.getString("ROUTE_DESTINATION", null)
            val stopsCount = sharedPreferences.getInt("ROUTE_STOPS_COUNT", 0)

            if (mMap == null) {
                return
            }

            stopLocations.clear()
            routePolyline?.remove()

            mMap?.clear()
            isRouteDisplayed = false

            if (!originAddress.isNullOrEmpty() && !destinationAddress.isNullOrEmpty()) {
                originLocation = findLocation(originAddress)
                destinationLocation = findLocation(destinationAddress)

                for (i in 0 until stopsCount) {
                    val stopAddress = sharedPreferences.getString("ROUTE_STOP_$i", null)
                    if (!stopAddress.isNullOrEmpty()) {
                        val stopLocation = findLocation(stopAddress)
                        if (stopLocation != null) {
                            stopLocations.add(stopLocation)
                        }
                    }
                }

                if (originLocation != null && destinationLocation != null) {
                    drawRoute(originLocation!!, destinationLocation!!, stopLocations)
                    preventLocationCameraUpdate = true
                    isRouteDisplayed = true
                } else {
                    Toast.makeText(requireContext(),"No se pudieron encontrar las ubicaciones",Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    //Encuentra la ubicación de una dirección
    private fun findLocation(address: String): LatLng? {
        try {
            val addresses = geocoder.getFromLocationName(address, 2)
            if (addresses != null && addresses.isNotEmpty()) {
                val addr = addresses[0]
                return LatLng(addr.latitude, addr.longitude)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(requireContext(), "Error al encontrar ubicación: ${e.message}", Toast.LENGTH_SHORT).show()
        }
        return null
    }

    //Dibuja la ruta
    private fun drawRoute(origin: LatLng, destination: LatLng, stops: List<LatLng>) {
        try {
            Toast.makeText(requireContext(), "Obteniendo ruta...", Toast.LENGTH_SHORT).show()
            getDirectionsData(origin, destination, stops)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(requireContext(),"Error al obtener la ruta: ${e.message}",Toast.LENGTH_SHORT).show()
            isRouteDisplayed = false
        }
    }

    //Obtiene las coordenadas de la ruta
    private fun getDirectionsData(
        origin: LatLng,
        destination: LatLng,
        waypoints: List<LatLng>
    ) {
        try {
            var urlString = "https://maps.googleapis.com/maps/api/directions/json?" +
                    "origin=${origin.latitude},${origin.longitude}" +
                    "&destination=${destination.latitude},${destination.longitude}" +
                    "&mode=driving"

            if (waypoints.isNotEmpty()) {
                urlString += "&waypoints="
                waypoints.forEachIndexed { index, point ->
                    urlString += "${point.latitude},${point.longitude}"
                    if (index < waypoints.size - 1) urlString += "|"
                }
            }

            urlString += "&key=$DIRECTIONS_API_KEY"

            //Uso de hilo para ejecutar la solicitud en segundo plano
            networkExecutor.execute {
                try {
                    val url = URL(urlString)
                    val connection = url.openConnection() as HttpURLConnection
                    connection.requestMethod = "GET"
                    connection.connectTimeout = 15000
                    connection.readTimeout = 15000

                    val reader = BufferedReader(InputStreamReader(connection.inputStream))
                    val response = StringBuilder()
                    var line: String?

                    while (reader.readLine().also { line = it } != null) {
                        response.append(line)
                    }

                    reader.close()

                    val jsonObject = JSONObject(response.toString())
                    val status = jsonObject.getString("status")

                    if (status == "OK") {
                        val routes = jsonObject.getJSONArray("routes")

                        if (routes.length() > 0) {
                            val legs = routes.getJSONObject(0).getJSONArray("legs")
                            val path = ArrayList<LatLng>()

                            for (i in 0 until legs.length()) {
                                val steps = legs.getJSONObject(i).getJSONArray("steps")

                                for (j in 0 until steps.length()) {
                                    val points = steps.getJSONObject(j).getJSONObject("polyline").getString("points")
                                    path.addAll(decodePoly(points))
                                }
                            }

                            requireActivity().runOnUiThread {
                                displayRouteOnMap(path, origin, destination, waypoints)
                            }
                        }
                    } else {
                        Log.e("DirectionsAPI", "Status no OK: $status")
                        requireActivity().runOnUiThread {
                            Toast.makeText(requireContext(), "Error al obtener la ruta: $status", Toast.LENGTH_SHORT).show()
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    Log.e("DirectionsAPI", "Error obteniendo ruta: ${e.message}")
                    requireActivity().runOnUiThread {
                        Toast.makeText(requireContext(), "Error al obtener la ruta: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e("DirectionsAPI", "Error obteniendo ruta: ${e.message}")
        }
    }

    private fun displayRouteOnMap(path: List<LatLng>, origin: LatLng, destination: LatLng, stops: List<LatLng>) {
        try {
            routePolyline?.remove()

            if (path.isNotEmpty()) {
                val polylineOptions = PolylineOptions()
                    .addAll(path)
                    .width(12f)
                    .color(Color.BLUE)
                    .geodesic(true)

                routePolyline = mMap?.addPolyline(polylineOptions)

                // Add markers
                mMap?.addMarker(
                    MarkerOptions()
                        .position(origin)
                        .title("Origen")
                )

                mMap?.addMarker(
                    MarkerOptions()
                        .position(destination)
                        .title("Destino")
                )

                stops.forEachIndexed { index, latLng ->
                    mMap?.addMarker(
                        MarkerOptions()
                            .position(latLng)
                            .title("Parada ${index + 1}")
                    )
                }

                val builder = LatLngBounds.Builder()
                builder.include(origin)
                builder.include(destination)
                stops.forEach { builder.include(it) }

                try {
                    val bounds = builder.build()
                    val padding = 100
                    val cameraUpdate = CameraUpdateFactory.newLatLngBounds(bounds, padding)
                    mMap?.moveCamera(cameraUpdate)
                } catch (e: Exception) {
                    mMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(origin, 12f))
                    e.printStackTrace()
                }

                isRouteDisplayed = true
                preventLocationCameraUpdate = true
            } else {
                Toast.makeText(requireContext(), "No se pudo trazar la ruta", Toast.LENGTH_SHORT).show()
                isRouteDisplayed = false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(requireContext(),"Error al obtener la ruta: ${e.message}",Toast.LENGTH_SHORT).show()
            isRouteDisplayed = false
        }
    }

    private fun decodePoly(encoded: String): List<LatLng> {
        val poly = ArrayList<LatLng>()
        var index = 0
        val len = encoded.length
        var lat = 0
        var lng = 0

        while (index < len) {
            var b: Int
            var shift = 0
            var result = 0

            do {
                b = encoded[index++].toInt() - 63
                result = result or (b and 0x1f shl shift)
                shift += 5
            } while (b >= 0x20)

            val dlat = if (result and 1 != 0) (result shr 1).inv() else result shr 1
            lat += dlat

            shift = 0
            result = 0

            do {
                b = encoded[index++].toInt() - 63
                result = result or (b and 0x1f shl shift)
                shift += 5
            } while (b >= 0x20)

            val dlng = if (result and 1 != 0) (result shr 1).inv() else result shr 1
            lng += dlng

            val p = LatLng(lat.toDouble() / 1E5, lng.toDouble() / 1E5)
            poly.add(p)
        }
        return poly
    }

    override fun onLocationChanged(location: Location) {
        currentLocation = location

        if (!preventLocationCameraUpdate && !isInitialLocationSet) {
            val currentLatLng = LatLng(location.latitude, location.longitude)
            mMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 15f))
            isInitialLocationSet = true
        }

        if (sharedPreferences.getBoolean("HAS_PUBLISHED_ROUTE", false) && !isRouteDisplayed && mMap != null) {
            loadRouteAndDisplay()
        }
    }

    private fun getCurrentLocationAndCenter() {
        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            try {
                val lastLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                    ?: locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)

                if (lastLocation != null) {
                    val currentLatLng = LatLng(lastLocation.latitude, lastLocation.longitude)
                    mMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 15f))
                    isInitialLocationSet = true
                    currentLocation = lastLocation
                } else {
                    // Si no hay última ubicación conocida, solicitar una nueva
                    locationManager.requestSingleUpdate(
                        LocationManager.GPS_PROVIDER,
                        object : LocationListener {
                            override fun onLocationChanged(location: Location) {
                                val currentLatLng = LatLng(location.latitude, location.longitude)
                                mMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 15f))
                                isInitialLocationSet = true
                                currentLocation = location
                            }
                        },
                        null
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Log.e("DriverHomeFragment", "Error getting current location: ${e.message}")
            }
        }
    }

    override fun onResume() {
        super.onResume()
        isInitialLocationSet = false
        startLocationUpdates()

        lightSensor?.let {
            sensorManager.registerListener(
                sensorEventListener,
                it,
                SensorManager.SENSOR_DELAY_NORMAL
            )
        }

        if (hasLocationPermission && mMap != null) {
            getCurrentLocationAndCenter()
        }

        if (sharedPreferences.getBoolean("HAS_ACTIVE_ROUTE", false)) {
            if (!isRouteDisplayed && mMap != null) {
                loadRouteAndDisplay()
            }
        }else {
            // Si no hay ruta activa, limpiar el mapa
            mMap?.clear()
            routePolyline?.remove()
            routePolyline = null
            isRouteDisplayed = false
            preventLocationCameraUpdate = false
        }

        updateUIForActiveRoute()
    }

    override fun onPause() {
        super.onPause()
        locationManager.removeUpdates(this)

        sensorManager.unregisterListener(sensorEventListener)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        locationManager.removeUpdates(this)
        sensorManager.unregisterListener(sensorEventListener)
        _binding = null
    }
}