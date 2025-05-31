package com.example.uniride.ui.driver.home

import android.Manifest
import android.content.Context
import android.content.Intent
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
import com.example.uniride.domain.model.Trip
import com.example.uniride.domain.model.TripStatus
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
    private val UPDATE_DISTANCE = 100f
    private var isInActiveTrip = false
    private val FINISH_TRIP_DISTANCE = 100f
    private var isNearDestination = false
    private var tripId: String? = null

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
    private val defaultLocation = LatLng(4.6097, -74.0817) // Bogotá, Colombia
    private var currentTrip: Trip? = null


    // Location variables
    private var originLocation: LatLng? = null
    private var destinationLocation: LatLng? = null
    private var lastUpdateLocation: Location? = null
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

                    updatePublishTripButton()

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

        binding.btnFinishTrip.setOnClickListener {
            showFinishTripDialog()
        }

        checkLocationPermission()
    }

    private fun updatePublishTripButton() {
        checkActiveTrip { hasActiveTrip ->
            if (hasActiveTrip) {
                binding.btnPublishTrip.visibility = View.GONE
            } else {
                binding.btnPublishTrip.visibility = View.VISIBLE
                binding.btnPublishTrip.text = "Publicar Viaje"
            }
        }
    }

    private fun checkActiveTrip(callback: (Boolean) -> Unit) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val db = FirebaseFirestore.getInstance()

        db.collection("trips")
            .whereEqualTo("idDriver", uid)
            .whereEqualTo("status", TripStatus.PENDING) // IN_PROGRESS según tu lógica
            .get()
            .addOnSuccessListener { documents ->
                callback(!documents.isEmpty)
            }
            .addOnFailureListener {
                callback(false)
            }
    }

    private fun updateFinishTripButton() {
        checkActiveTrip { hasActiveTrip ->
            if (hasActiveTrip && isNearDestination) {
                binding.btnFinishTrip.visibility = View.VISIBLE
                binding.btnPublishTrip.visibility = View.GONE
            } else {
                binding.btnFinishTrip.visibility = View.GONE
                if (!hasActiveTrip) {
                    binding.btnPublishTrip.visibility = View.VISIBLE
                }
            }
        }
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
                    // Usar intervalos más frecuentes durante viaje activo
                    val minTime = if (isInActiveTrip) 2000L else 5000L // 2s vs 5s
                    val minDistance = if (isInActiveTrip) 5f else 10f // 5m vs 10m

                    locationManager.requestLocationUpdates(
                        LocationManager.GPS_PROVIDER,
                        minTime,
                        minDistance,
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
        updatePublishTripButton()

        checkActiveTrip { hasActiveTrip ->
            if (hasActiveTrip) {
                loadRouteAndDisplay()
            }
        }
    }

    private fun loadRouteAndDisplay() {
        loadActiveTrip { trip ->
            if (trip != null) {
                // Cargar la ruta usando los datos del trip de Firestore
                loadRouteFromTrip(trip)
            }
        }
    }

    private fun loadActiveTrip(callback: (Trip?) -> Unit) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val db = FirebaseFirestore.getInstance()

        db.collection("trips")
            .whereEqualTo("idDriver", uid)
            .whereEqualTo("status", TripStatus.PENDING)
            .get()
            .addOnSuccessListener { documents ->
                if (!documents.isEmpty) {
                    val tripData = documents.documents[0].data
                    val trip = tripData?.let {
                        Trip(
                            id = documents.documents[0].id,
                            idDriver = it["idDriver"] as? String ?: "",
                            idRoute = it["idRoute"] as? String ?: "",
                            description = it["description"] as? String ?: "",
                            price = it["price"] as? Double ?: 0.0,
                            date = it["date"] as? String ?: "",
                            startTime = it["startTime"] as? String ?: "",
                            status = TripStatus.valueOf(it["status"] as? String ?: "PENDING")
                        )
                    }
                    callback(trip)
                } else {
                    callback(null)
                }
            }
            .addOnFailureListener {
                callback(null)
            }
    }

    private fun loadRouteFromTrip(trip: Trip) {
        val db = FirebaseFirestore.getInstance()

        // Cargar los datos de la ruta desde Firestore usando el idRoute del trip
        db.collection("routes").document(trip.idRoute)
            .get()
            .addOnSuccessListener { routeDocument ->
                if (routeDocument.exists()) {
                    val routeData = routeDocument.data

                    // Extraer origen, destino y paradas
                    val originMap = routeData?.get("origin") as? Map<String, Double>
                    val destinationMap = routeData?.get("destination") as? Map<String, Double>
                    val stopsArray = routeData?.get("stops") as? List<Map<String, Double>>

                    if (originMap != null && destinationMap != null) {
                        originLocation = LatLng(originMap["latitude"] ?: 0.0, originMap["longitude"] ?: 0.0)
                        destinationLocation = LatLng(destinationMap["latitude"] ?: 0.0, destinationMap["longitude"] ?: 0.0)

                        // Cargar paradas si existen
                        stopLocations.clear()
                        stopsArray?.forEach { stopMap ->
                            val lat = stopMap["latitude"] ?: 0.0
                            val lng = stopMap["longitude"] ?: 0.0
                            stopLocations.add(LatLng(lat, lng))
                        }

                        // Dibujar la ruta
                        drawRoute(originLocation!!, destinationLocation!!, stopLocations)
                        isInActiveTrip = true
                    }
                }
            }
            .addOnFailureListener { e ->
                Log.e("RouteLoad", "Error cargando ruta: ${e.message}")
                Toast.makeText(requireContext(), "Error cargando ruta del viaje", Toast.LENGTH_SHORT).show()
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

    private fun checkDistanceToDestination(currentLocation: Location) {
        if (destinationLocation == null) return

        val destinationLocationObj = Location("destination").apply {
            latitude = destinationLocation!!.latitude
            longitude = destinationLocation!!.longitude
        }

        val distanceToDestination = currentLocation.distanceTo(destinationLocationObj)

        Log.d("TripFinish", "Distancia al destino: ${distanceToDestination}m")

        val wasNearDestination = isNearDestination
        isNearDestination = distanceToDestination <= FINISH_TRIP_DISTANCE

        // Solo actualizar UI si cambió el estado
        if (wasNearDestination != isNearDestination) {
            updateFinishTripButton()

            if (isNearDestination) {
                Toast.makeText(
                    requireContext(),
                    "¡Has llegado cerca del destino! Puedes terminar el viaje.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun showFinishTripDialog() {
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Terminar Viaje")
            .setMessage("¿Estás seguro de que quieres terminar el viaje? Esta acción no se puede deshacer.")
            .setPositiveButton("Sí, Terminar") { _, _ ->
                finishTrip()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun deleteFromActiveTrips(tripId: String, db: FirebaseFirestore, loadingDialog: androidx.appcompat.app.AlertDialog) {
        db.collection("active_trips").document(tripId)
            .delete()
            .addOnSuccessListener {
                Log.d("TripFinish", "Viaje eliminado de active_trips exitosamente")
                loadingDialog.dismiss()
                // Mostrar mensaje de éxito
                showTripCompletedDialog()
            }
            .addOnFailureListener { e ->
                loadingDialog.dismiss()
                Log.e("TripFinish", "Error eliminando viaje de active_trips: ${e.message}")
                Toast.makeText(requireContext(), "Error al limpiar datos del viaje: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun showTripCompletedDialog() {
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("¡Viaje Completado!")
            .setMessage("Has completado exitosamente el viaje. Gracias por usar UniRide como conductor.")
            .setPositiveButton("Ver Historial") { _, _ ->
                findNavController().navigate(R.id.driverTripsFragment)
            }
            .setNegativeButton("Continuar") { _, _ ->
            }
            .setCancelable(false)
            .show()
    }

    override fun onLocationChanged(location: Location) {
        currentLocation = location

        if (!preventLocationCameraUpdate && !isInitialLocationSet && !isRouteDisplayed) {
            val currentLatLng = LatLng(location.latitude, location.longitude)
            mMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 15f))
            isInitialLocationSet = true
        }

        checkAndUpdateRouteIfNeeded(location)

        if (isInActiveTrip) {
            checkDistanceToDestination(location)
        }

        // Cambiar esta verificación:
        checkActiveTrip { hasActiveTrip ->
            if (hasActiveTrip && !isRouteDisplayed && mMap != null) {
                loadRouteAndDisplay()
            }
        }
    }

    private fun checkAndUpdateRouteIfNeeded(currentLocation: Location) {
        checkActiveTrip { hasActiveTrip ->
            isInActiveTrip = hasActiveTrip

            if (!isInActiveTrip || destinationLocation == null) {
                return@checkActiveTrip
            }

            val distanceFromLast = if (lastUpdateLocation != null) {
                currentLocation.distanceTo(lastUpdateLocation!!)
            } else {
                Float.MAX_VALUE
            }

            if (lastUpdateLocation == null || distanceFromLast >= UPDATE_DISTANCE) {
                lastUpdateLocation = Location(currentLocation)
                updateDriverLocationInFirestore(currentLocation)
                updateRouteFromCurrentLocation(currentLocation)
            }
        }
    }

    private fun updateDriverLocationInFirestore(location: Location) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val db = FirebaseFirestore.getInstance()

        val locationData = hashMapOf(
            "latitude" to location.latitude,
            "longitude" to location.longitude,
            "timestamp" to System.currentTimeMillis(),
            "lastUpdate" to com.google.firebase.Timestamp.now()
        )

        // Actualizar la ubicación del conductor in tiempo real
        db.collection("active_trips")
            .whereEqualTo("driverId", uid)
            .get()
            .addOnSuccessListener { documents ->
                for (document in documents) {
                    document.reference.update("driverLocation", locationData)
                        .addOnSuccessListener {
                        }
                        .addOnFailureListener { e ->
                        }
                }
            }
            .addOnFailureListener { e ->
                Log.e("DriverLocation", "Error buscando viajes activos: ${e.message}")
            }
    }

    private fun updateRouteFromCurrentLocation(currentLocation: Location) {
        val currentLatLng = LatLng(currentLocation.latitude, currentLocation.longitude)

        if (destinationLocation == null) {
            return
        }

        // Filtrar las paradas que aún no se han alcanzado
        val remainingStops = filterRemainingStops(currentLatLng, stopLocations)
        // Limpiar la ruta anterior
        routePolyline?.remove()

        // Redibujar la ruta desde la ubicación actual hasta el destino
        drawRouteFromCurrentPosition(currentLatLng, destinationLocation!!, remainingStops)
    }

    private fun drawRouteFromCurrentPosition(origin: LatLng, destination: LatLng, stops: List<LatLng>) {
        try {
            Toast.makeText(requireContext(), "Actualizando ruta...", Toast.LENGTH_SHORT).show()
            getDirectionsDataForUpdate(origin, destination, stops)
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e("RouteUpdate", "Error al actualizar la ruta: ${e.message}")
            Toast.makeText(requireContext(), "Error al actualizar la ruta: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun getDirectionsDataForUpdate(
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
                                displayUpdatedRouteOnMap(path, origin, destination, waypoints)
                            }
                        }
                    } else {
                        requireActivity().runOnUiThread {
                            Toast.makeText(requireContext(), "Error al actualizar la ruta: $status", Toast.LENGTH_SHORT).show()
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    requireActivity().runOnUiThread {
                        Toast.makeText(requireContext(), "Error al actualizar la ruta: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e("RouteUpdate", "Error obteniendo ruta actualizada: ${e.message}")
        }
    }

    private fun displayUpdatedRouteOnMap(path: List<LatLng>, origin: LatLng, destination: LatLng, stops: List<LatLng>) {
        try {
            // Remover la ruta anterior
            routePolyline?.remove()

            // Limpiar marcadores anteriores (excepto la ubicación actual del usuario)
            mMap?.clear()

            if (path.isNotEmpty()) {
                val polylineOptions = PolylineOptions()
                    .addAll(path)
                    .width(12f)
                    .color(Color.BLUE)
                    .geodesic(true)

                routePolyline = mMap?.addPolyline(polylineOptions)

                // Agregar marcador de ubicación actual (conductor)
                mMap?.addMarker(MarkerOptions().position(origin).title("Tu ubicación actual"))

                // Agregar marcador de destino
                mMap?.addMarker(MarkerOptions().position(destination).title("Destino"))

                // Agregar marcadores de paradas restantes
                stops.forEachIndexed { index, latLng ->
                    mMap?.addMarker(MarkerOptions().position(latLng).title("Parada ${index + 1}"))
                }

                // Ajustar la cámara para mostrar toda la ruta actualizada
                val builder = LatLngBounds.Builder()
                builder.include(origin)
                builder.include(destination)
                stops.forEach { builder.include(it) }

                try {
                    val bounds = builder.build()
                    val padding = 150
                    val cameraUpdate = CameraUpdateFactory.newLatLngBounds(bounds, padding)
                    mMap?.animateCamera(cameraUpdate)
                } catch (e: Exception) {
                    mMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(origin, 14f))
                    e.printStackTrace()
                }

                isRouteDisplayed = true
                Toast.makeText(requireContext(), "Ruta actualizada", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(requireContext(), "No se pudo actualizar la ruta", Toast.LENGTH_SHORT).show()
                isRouteDisplayed = false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e("RouteUpdate", "Error al mostrar la ruta actualizada: ${e.message}")
            Toast.makeText(requireContext(), "Error al mostrar la ruta actualizada: ${e.message}", Toast.LENGTH_SHORT).show()
            isRouteDisplayed = false
        }
    }

    private fun filterRemainingStops(currentLocation: LatLng, allStops: List<LatLng>): List<LatLng> {
        val remainingStops = mutableListOf<LatLng>()
        val currentLocationObj = Location("current").apply {
            latitude = currentLocation.latitude
            longitude = currentLocation.longitude
        }

        for (stop in allStops) {
            val stopLocation = Location("stop").apply {
                latitude = stop.latitude
                longitude = stop.longitude
            }

            // Si la parada está a más de 300m, considerarla como pendiente
            // Reducimos la distancia para ser más precisos
            val distanceToStop = currentLocationObj.distanceTo(stopLocation)
            Log.d("RouteUpdate", "Distancia a parada: ${distanceToStop}m")

            if (distanceToStop > 300f) {
                remainingStops.add(stop)
            }
        }

        return remainingStops
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

    private fun finishTrip() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val db = FirebaseFirestore.getInstance()

        val loadingDialog = androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setMessage("Finalizando viaje...")
            .setCancelable(false)
            .create()
        loadingDialog.show()

        // Buscar trip activo en la colección "trips"
        db.collection("trips")
            .whereEqualTo("idDriver", uid)
            .whereEqualTo("status", TripStatus.PENDING.name)
            .get()
            .addOnSuccessListener { documents ->
                if (!documents.isEmpty) {
                    val tripDoc = documents.documents[0]
                    updateTripStatusToTerminated(tripDoc.id, db, loadingDialog)
                } else {
                    loadingDialog.dismiss()
                    Toast.makeText(requireContext(), "No se encontró un viaje activo", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                loadingDialog.dismiss()
                Toast.makeText(requireContext(), "Error al buscar viaje activo", Toast.LENGTH_SHORT).show()
            }
    }

    private fun updateTripStatusToTerminated(tripId: String, db: FirebaseFirestore, loadingDialog: androidx.appcompat.app.AlertDialog) {
        val updateData = hashMapOf<String, Any>(
            "status" to TripStatus.TERMINATED.name
        )

        db.collection("trips").document(tripId)
            .update(updateData)
            .addOnSuccessListener {
                loadingDialog.dismiss()
                showTripCompletedDialog()
            }
            .addOnFailureListener { e ->
                loadingDialog.dismiss()
                Toast.makeText(requireContext(), "Error al finalizar viaje", Toast.LENGTH_SHORT).show()
            }
    }

    private fun updateTripStatus(tripId: String, db: FirebaseFirestore, loadingDialog: androidx.appcompat.app.AlertDialog) {
        val updateData = hashMapOf<String, Any>(
            "status" to "TERMINATED",
            "endTime" to com.google.firebase.Timestamp.now(),
            "completedAt" to System.currentTimeMillis()
        )

        db.collection("active_trips").document(tripId)
            .update(updateData)
            .addOnSuccessListener {
                Log.d("TripFinish", "Estado del viaje actualizado a 'TERMINATED'")

                // Mover el viaje a historial
                moveToTripHistory(tripId, db, loadingDialog)
            }
            .addOnFailureListener { e ->
                loadingDialog.dismiss()
                Log.e("TripFinish", "Error actualizando estado del viaje: ${e.message}")
                Toast.makeText(requireContext(), "Error al finalizar viaje", Toast.LENGTH_SHORT).show()
            }
    }

    private fun moveToTripHistory(tripId: String, db: FirebaseFirestore, loadingDialog: androidx.appcompat.app.AlertDialog) {
        // Obtener los datos del viaje
        db.collection("active_trips").document(tripId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val tripData = document.data?.toMutableMap() ?: mutableMapOf()

                    // Añadir información adicional para el historial
                    tripData["archivedAt"] = com.google.firebase.Timestamp.now()
                    tripData["finalStatus"] = "FINISHED"
                    tripData["completionMethod"] = "DRIVER_FINISHED" // Indicar que fue el conductor quien terminó

                    // Agregar estadísticas del viaje si están disponibles
                    currentLocation?.let { location ->
                        tripData["finalDriverLocation"] = hashMapOf(
                            "latitude" to location.latitude,
                            "longitude" to location.longitude
                        )
                    }

                    // Guardar en el historial
                    db.collection("trip_history").document(tripId)
                        .set(tripData)
                        .addOnSuccessListener {
                            Log.d("TripFinish", "Viaje movido al historial exitosamente")

                            // Eliminar de viajes activos
                            deleteFromActiveTrips(tripId, db, loadingDialog)
                        }
                        .addOnFailureListener { e ->
                            loadingDialog.dismiss()
                            Log.e("TripFinish", "Error moviendo viaje al historial: ${e.message}")
                            Toast.makeText(requireContext(), "Error al archivar viaje: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                } else {
                    loadingDialog.dismiss()
                    Log.e("TripFinish", "El documento del viaje no existe")
                    Toast.makeText(requireContext(), "Error: El viaje no existe", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                loadingDialog.dismiss()
                Log.e("TripFinish", "Error obteniendo datos del viaje: ${e.message}")
                Toast.makeText(requireContext(), "Error al obtener datos del viaje: ${e.message}", Toast.LENGTH_SHORT).show()
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

        updatePublishTripButton()
        updateFinishTripButton()

        // Cambiar esta parte:
        checkActiveTrip { hasActiveTrip ->
            isInActiveTrip = hasActiveTrip

            if (hasActiveTrip) {
                if (!isRouteDisplayed && mMap != null) {
                    loadRouteAndDisplay()
                }
            } else {
                mMap?.clear()
                routePolyline?.remove()
                routePolyline = null
                isRouteDisplayed = false
                preventLocationCameraUpdate = false
                isInActiveTrip = false
                isNearDestination = false
                lastUpdateLocation = null
            }
        }
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