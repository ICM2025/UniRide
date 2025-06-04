package com.example.uniride.ui.passenger.home

import android.Manifest
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.uniride.R
import com.example.uniride.databinding.FragmentPassengerHomeBinding
import com.example.uniride.ui.passenger.search.SearchFlowActivity
import com.example.uniride.utility.Config_permission
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import java.util.concurrent.Executors
import java.net.URL
import java.net.HttpURLConnection
import java.io.BufferedReader
import java.io.InputStreamReader
import org.json.JSONObject
import java.util.ArrayList
import android.content.pm.PackageManager
import android.location.LocationListener
import android.location.LocationManager
import android.os.Handler
import android.os.Looper
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.navigation.fragment.findNavController

class PassengerHomeFragment : Fragment(), OnMapReadyCallback, LocationListener {

    private var _binding: FragmentPassengerHomeBinding? = null
    private val binding get() = _binding!!

    private lateinit var bottomSheetBehavior: BottomSheetBehavior<View>

    // Location components
    private lateinit var locationManager: LocationManager
    private val locationPermissionCode = 1001
    private var hasLocationPermission = false
    private var currentLocation: Location? = null
    private lateinit var gpsPermissionLauncher: ActivityResultLauncher<String>
    private lateinit var notificationPermissionLauncher: ActivityResultLauncher<String>
    private var lastDriverLocationUpdate: LatLng? = null
    private val DRIVER_UPDATE_DISTANCE = 100f

    // Map components
    private var mMap: GoogleMap? = null
    private var routePolyline: Polyline? = null
    private var driverMarker: Marker? = null
    private var originMarker: Marker? = null
    private var destinationMarker: Marker? = null
    private var stopMarkers: MutableList<Marker> = mutableListOf()

    // Trip tracking
    private var currentTripId: String? = null
    private var tripListener: ListenerRegistration? = null
    private var isInActiveTrip = false

    // Locations
    private var originLocation: LatLng? = null
    private var destinationLocation: LatLng? = null
    private var driverLocation: LatLng? = null
    private var stopLocations: MutableList<LatLng> = mutableListOf()

    // Network executor
    private val networkExecutor = Executors.newSingleThreadExecutor()

    // Flag para evitar múltiples configuraciones del listener
    private var destinationListenerConfigured = false

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
        _binding = FragmentPassengerHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        locationManager =
            requireActivity().getSystemService(Context.LOCATION_SERVICE) as LocationManager
        setupPermissionLauncher()

        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment?
        mapFragment?.getMapAsync(this)

        setupBottomSheet()
        setupDestinationClickListener()
        checkLocationPermission()

        // Verificar si hay un viaje activo al iniciar
        checkForActiveTrip()
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

    private fun setupDestinationClickListener() {
        // Configurar el listener solo una vez
        if (!destinationListenerConfigured && !isInActiveTrip) {
            binding.etDestination.setOnClickListener {
                if (!isInActiveTrip) {
                    val intent = Intent(requireContext(), SearchFlowActivity::class.java)
                    startActivity(intent)
                }
            }
            destinationListenerConfigured = true
        }
    }

    private fun setupPermissionLauncher() {
        gpsPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted ->
            if (isGranted) {
                hasLocationPermission = true
                startLocationUpdates()
                enableMyLocation()
            } else {
                Toast.makeText(
                    requireContext(),
                    "La ubicación es necesaria para mostrar tu posición en el mapa",
                    Toast.LENGTH_LONG
                ).show()
            }

            // Después de GPS, pedimos notificaciones
            checkNotificationPermission()
        }

        notificationPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted ->
            if (isGranted) {
                Toast.makeText(requireContext(), "Permiso de notificaciones concedido", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(requireContext(), "Permiso de notificaciones denegado", Toast.LENGTH_SHORT).show()
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
            checkNotificationPermission()
        } else {
            gpsPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }
    private fun checkNotificationPermission() {
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            if (shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)) {
                Toast.makeText(
                    requireContext(),
                    "El permiso de notificaciones es necesario para avisarte sobre solicitudes y viajes.",
                    Toast.LENGTH_LONG
                ).show()
            }
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
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
                        5000L, // 5 segundos
                        10f,   // 10 metros
                        this
                    )

                    val lastLocation =
                        locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                    if (lastLocation != null) {
                        onLocationChanged(lastLocation)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(
                    requireContext(),
                    "Error al iniciar actualizaciones de ubicación: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
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
            }
        }
    }

    override fun onLocationChanged(location: Location) {
        currentLocation = location

        // Centrar el mapa en la ubicación actual
        centerMapOnCurrentLocation(location)

        // Solo configurar el listener si no estamos en un viaje activo
        if (!isInActiveTrip && !destinationListenerConfigured) {
            setupDestinationClickListener()
        }
    }

    private fun centerMapOnCurrentLocation(location: Location) {
        mMap?.let { map ->
            val currentLatLng = LatLng(location.latitude, location.longitude)

            // Solo centrar si no hay una ruta activa dibujada
            if (!isInActiveTrip) {
                map.animateCamera(
                    CameraUpdateFactory.newLatLngZoom(currentLatLng, 15f),
                    1000, // Duración de la animación en ms
                    null
                )
                Log.d("PassengerHome", "Mapa centrado en ubicación actual: $currentLatLng")
            }
        }
    }


    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        googleMap.uiSettings.apply {
            isZoomControlsEnabled = true
            isMapToolbarEnabled = true
        }

        enableMyLocation()

        // Si ya tenemos una ubicación actual, centrar el mapa
        currentLocation?.let { location ->
            centerMapOnCurrentLocation(location)
        }
    }

    private fun checkForActiveTrip() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val db = FirebaseFirestore.getInstance()

        Log.d("PassengerHome", "Verificando viaje activo para usuario: $uid")

        // Buscar solicitudes ACCEPTED del usuario
        db.collection("PassengerRequests")
            .whereEqualTo("idUser", uid)
            .whereEqualTo("status", "ACCEPTED")
            .get()
            .addOnSuccessListener { passengerRequests ->
                Log.d("PassengerHome", "Documentos encontrados: ${passengerRequests.size()}")

                if (passengerRequests.isEmpty) {
                    Log.d("PassengerHome", "No hay solicitudes ACCEPTED")
                    if (isInActiveTrip) {
                        resetUIForNormalState()
                    }
                    return@addOnSuccessListener
                }

                // Verificar cada solicitud para encontrar un trip ACTIVE
                var foundActiveTrip = false
                var requestsChecked = 0
                val totalRequests = passengerRequests.size()

                for (requestDoc in passengerRequests) {
                    val tripId = requestDoc.getString("idTrip")
                    Log.d("PassengerHome", "Solicitud ACCEPTED encontrada para trip: $tripId")

                    if (tripId != null) {
                        // Verificar si el trip existe y está ACTIVE
                        db.collection("Trips").document(tripId)
                            .get()
                            .addOnSuccessListener { tripDoc ->
                                requestsChecked++
                                Log.d("PassengerHome", "Verificando status del trip: $tripId")
                                Log.d("PassengerHome", "Document exists: ${tripDoc.exists()}")

                                if (tripDoc.exists()) {
                                    val status = tripDoc.getString("status")
                                    Log.d("PassengerHome", "Trip $tripId tiene status: $status")

                                    if (status == "ACTIVE" && !foundActiveTrip) {
                                        foundActiveTrip = true
                                        Log.d("PassengerHome", "¡Trip ACTIVE encontrado! $tripId")
                                        currentTripId = tripId
                                        isInActiveTrip = true

                                        // Cargar la ruta primero, luego configurar el tracking
                                        loadTripRoute(tripId) {
                                            setupTripTracking(tripId)
                                            updateUIForActiveTrip()
                                        }
                                    }
                                } else {
                                    Log.w("PassengerHome", "Trip $tripId no existe en la colección Trips")
                                }

                                // Si ya verificamos todas las solicitudes y no encontramos trip activo
                                if (requestsChecked >= totalRequests && !foundActiveTrip && isInActiveTrip) {
                                    Log.d("PassengerHome", "No se encontró trip ACTIVE, reseteando UI")
                                    resetUIForNormalState()
                                }
                            }
                            .addOnFailureListener { e ->
                                requestsChecked++
                                Log.e("PassengerHome", "Error verificando trip $tripId: ${e.message}")

                                if (requestsChecked >= totalRequests && !foundActiveTrip && isInActiveTrip) {
                                    resetUIForNormalState()
                                }
                            }
                    } else {
                        requestsChecked++
                        Log.w("PassengerHome", "idTrip es null en solicitud")

                        if (requestsChecked >= totalRequests && !foundActiveTrip && isInActiveTrip) {
                            resetUIForNormalState()
                        }
                    }
                }
            }
            .addOnFailureListener { e ->
                Log.e("PassengerHome", "Error buscando solicitudes ACCEPTED: ${e.message}")
                if (isInActiveTrip) {
                    resetUIForNormalState()
                }
            }
    }

    private fun setupTripTracking(tripId: String) {
        val db = FirebaseFirestore.getInstance()

        // Remover listener anterior si existe
        tripListener?.remove()

        Log.d("PassengerHome", "Configurando listener para trip: $tripId")

        // Listener para actualizaciones en tiempo real del trip
        tripListener = db.collection("Trips").document(tripId)
            .addSnapshotListener { document, error ->
                if (error != null) {
                    Log.e("PassengerHome", "Error en listener: ${error.message}")
                    return@addSnapshotListener
                }

                if (document != null && document.exists()) {
                    val status = document.getString("status")
                    Log.d("PassengerHome", "Trip actualizado - Status: $status")

                    when (status) {
                        "ACTIVE" -> {
                            // Actualizar ubicación del conductor si está disponible
                            val driverLocationData = document.get("driverLocation") as? Map<String, Any>
                            if (driverLocationData != null) {
                                // Verificar tanto 'latitude/longitude' como 'lat/lng'
                                val lat = (driverLocationData["latitude"] ?: driverLocationData["lat"]) as? Double
                                val lng = (driverLocationData["longitude"] ?: driverLocationData["lng"]) as? Double

                                if (lat != null && lng != null) {
                                    val driverLatLng = LatLng(lat, lng)
                                    Log.d("PassengerHome", "Actualizando ubicación conductor: $driverLatLng")
                                    updateDriverLocation(driverLatLng)
                                } else {
                                    Log.w("PassengerHome", "Coordenadas del conductor no válidas: $driverLocationData")
                                }
                            } else {
                                Log.d("PassengerHome", "No hay datos de ubicación del conductor disponibles")
                            }
                        }

                        "TERMINATED", "COMPLETED" -> {
                            Log.d("PassengerHome", "El viaje terminó con status: $status")
                            handleTripTerminated()
                        }
                    }
                } else {
                    Log.w("PassengerHome", "Document no existe o fue eliminado")
                    if (isInActiveTrip) {
                        handleTripTerminated()
                    }
                }
            }
    }

    private fun loadTripRoute(tripId: String, onComplete: (() -> Unit)? = null) {
        val db = FirebaseFirestore.getInstance()

        Log.d("PassengerHome", "Cargando ruta para trip: $tripId")

        db.collection("Trips").document(tripId)
            .get()
            .addOnSuccessListener { tripDoc ->
                if (tripDoc.exists()) {
                    val routeId = tripDoc.getString("idRoute")
                    val driverLocationData = tripDoc.get("driverLocation") as? Map<String, Any>

                    Log.d("PassengerHome", "Route ID encontrado: $routeId")

                    // Obtener ubicación inicial del conductor si está disponible
                    if (driverLocationData != null) {
                        val lat = (driverLocationData["latitude"] ?: driverLocationData["lat"]) as? Double
                        val lng = (driverLocationData["longitude"] ?: driverLocationData["lng"]) as? Double

                        if (lat != null && lng != null) {
                            driverLocation = LatLng(lat, lng)
                            Log.d("PassengerHome", "Ubicación inicial del conductor: $driverLocation")
                        }
                    }

                    if (routeId != null) {
                        loadRouteData(routeId, onComplete)
                    } else {
                        Log.w("PassengerHome", "No se encontró idRoute en el trip")
                        onComplete?.invoke()
                    }
                } else {
                    Log.w("PassengerHome", "Trip document no existe")
                    onComplete?.invoke()
                }
            }
            .addOnFailureListener { e ->
                Log.e("PassengerHome", "Error cargando trip: ${e.message}")
                onComplete?.invoke()
            }
    }


    private fun loadRouteData(routeId: String, onComplete: (() -> Unit)? = null) {
        val db = FirebaseFirestore.getInstance()

        Log.d("PassengerHome", "Cargando datos de ruta: $routeId")

        db.collection("Routes").document(routeId)
            .get()
            .addOnSuccessListener { routeDocument ->
                if (routeDocument.exists()) {
                    val routeData = routeDocument.data
                    val idOrigin = routeData?.get("idOrigin") as? String
                    val idDestination = routeData?.get("idDestination") as? String
                    val stopsArray = routeData?.get("stops") as? List<String>

                    Log.d(
                        "PassengerHome",
                        "Origen: $idOrigin, Destino: $idDestination, Stops: ${stopsArray?.size ?: 0}"
                    )

                    if (idOrigin != null && idDestination != null) {
                        // Usar contador para sincronizar la carga
                        var loadedCount = 0
                        val totalToLoad = 2 + (stopsArray?.size ?: 0)

                        var tempOrigin: LatLng? = null
                        var tempDestination: LatLng? = null
                        val tempStops = mutableListOf<LatLng>()

                        fun checkAllLoaded() {
                            loadedCount++
                            if (loadedCount >= totalToLoad) {
                                if (tempOrigin != null && tempDestination != null) {
                                    originLocation = tempOrigin
                                    destinationLocation = tempDestination
                                    stopLocations.clear()
                                    stopLocations.addAll(tempStops)

                                    Log.d(
                                        "PassengerHome",
                                        "Todas las ubicaciones cargadas, dibujando ruta"
                                    )
                                    drawRoute(
                                        originLocation!!,
                                        destinationLocation!!,
                                        stopLocations
                                    )
                                    onComplete?.invoke()
                                } else {
                                    Log.e("PassengerHome", "Error: origen o destino son null")
                                    onComplete?.invoke()
                                }
                            }
                        }

                        // Cargar origen
                        loadLocationFromId(idOrigin, "Stops") { originLatLng ->
                            tempOrigin = originLatLng
                            Log.d("PassengerHome", "Origen cargado: $originLatLng")
                            checkAllLoaded()
                        }

                        // Cargar destino
                        loadLocationFromId(idDestination, "Stops") { destinationLatLng ->
                            tempDestination = destinationLatLng
                            Log.d("PassengerHome", "Destino cargado: $destinationLatLng")
                            checkAllLoaded()
                        }

                        // Cargar paradas si existen
                        if (!stopsArray.isNullOrEmpty()) {
                            for (stopId in stopsArray) {
                                loadLocationFromId(stopId, "Stops") { stopLatLng ->
                                    if (stopLatLng != null) {
                                        tempStops.add(stopLatLng)
                                    }
                                    checkAllLoaded()
                                }
                            }
                        } else {
                            // No hay paradas, marcar como completadas
                            loadedCount += 0 // No hay paradas que cargar
                        }
                    } else {
                        Log.e("PassengerHome", "idOrigin o idDestination son null")
                        onComplete?.invoke()
                    }
                } else {
                    Log.e("PassengerHome", "Route document no existe")
                    onComplete?.invoke()
                }
            }
            .addOnFailureListener { e ->
                Log.e("PassengerHome", "Error cargando ruta: ${e.message}")
                onComplete?.invoke()
            }
    }

    private fun loadLocationFromId(
        locationId: String,
        collection: String,
        callback: (LatLng?) -> Unit
    ) {
        val db = FirebaseFirestore.getInstance()

        Log.d("PassengerHome", "Cargando ubicación $locationId de colección $collection")

        db.collection(collection).document(locationId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val data = document.data
                    Log.d("PassengerHome", "Datos del documento $locationId: $data")

                    // Intentar primero con lat/lng (como aparece en tus datos de Firebase)
                    var latitude = data?.get("lat") as? Double
                    var longitude = data?.get("lng") as? Double

                    // Si no funciona, intentar con latitude/longitude
                    if (latitude == null || longitude == null) {
                        latitude = data?.get("latitude") as? Double
                        longitude = data?.get("longitude") as? Double
                    }

                    if (latitude != null && longitude != null) {
                        Log.d("PassengerHome", "Ubicación cargada: ($latitude, $longitude)")
                        callback(LatLng(latitude, longitude))
                    } else {
                        Log.e(
                            "PassengerHome",
                            "Coordenadas no encontradas en documento $locationId"
                        )
                        Log.d("PassengerHome", "Claves disponibles: ${data?.keys}")
                        callback(null)
                    }
                } else {
                    Log.e("PassengerHome", "Documento $locationId no existe en $collection")
                    callback(null)
                }
            }
            .addOnFailureListener { e ->
                Log.e("PassengerHome", "Error cargando $locationId: ${e.message}")
                callback(null)
            }
    }

    private fun drawRoute(origin: LatLng, destination: LatLng, stops: List<LatLng>) {
        Log.d(
            "PassengerHome",
            "Dibujando ruta desde $origin hasta $destination con ${stops.size} paradas"
        )
        getDirectionsData(origin, destination, stops)
    }

    private fun getDirectionsData(origin: LatLng, destination: LatLng, waypoints: List<LatLng>) {
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

            Log.d("PassengerHome", "Solicitando direcciones: $urlString")

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

                    Log.d("PassengerHome", "Respuesta API status: $status")

                    if (status == "OK") {
                        val routes = jsonObject.getJSONArray("routes")

                        if (routes.length() > 0) {
                            val legs = routes.getJSONObject(0).getJSONArray("legs")
                            val path = ArrayList<LatLng>()

                            for (i in 0 until legs.length()) {
                                val steps = legs.getJSONObject(i).getJSONArray("steps")

                                for (j in 0 until steps.length()) {
                                    val points = steps.getJSONObject(j).getJSONObject("polyline")
                                        .getString("points")
                                    path.addAll(decodePoly(points))
                                }
                            }

                            Log.d("PassengerHome", "Ruta decodificada con ${path.size} puntos")

                            requireActivity().runOnUiThread {
                                displayRouteOnMap(path, origin, destination, waypoints)
                            }
                        }
                    } else {
                        Log.e("PassengerHome", "Error en API Directions: $status")
                    }
                } catch (e: Exception) {
                    Log.e("PassengerHome", "Error en red: ${e.message}")
                    e.printStackTrace()
                }
            }
        } catch (e: Exception) {
            Log.e("PassengerHome", "Error configurando request: ${e.message}")
            e.printStackTrace()
        }
    }

    private fun displayRouteOnMap(
        path: List<LatLng>,
        origin: LatLng,
        destination: LatLng,
        stops: List<LatLng>
    ) {
        try {
            Log.d("PassengerHome", "Mostrando ruta en mapa con ${path.size} puntos")

            // Limpiar marcadores de ruta previos, mantener el marcador del driver
            clearRouteMarkersOnly()
            routePolyline?.remove()

            if (path.isNotEmpty()) {
                // Dibujar polyline
                val polylineOptions = PolylineOptions()
                    .addAll(path)
                    .width(8f)
                    .color(Color.parseColor("#1976D2"))
                    .geodesic(true)

                routePolyline = mMap?.addPolyline(polylineOptions)

                // Marcador de origen
                originMarker = mMap?.addMarker(
                    MarkerOptions()
                        .position(origin)
                        .title("Origen")
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN))
                )

                // Marcador de destino
                destinationMarker = mMap?.addMarker(
                    MarkerOptions()
                        .position(destination)
                        .title("Destino")
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))
                )

                // Marcadores de paradas
                stops.forEachIndexed { index, latLng ->
                    val stopMarker = mMap?.addMarker(
                        MarkerOptions()
                            .position(latLng)
                            .title("Parada ${index + 1}")
                            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_YELLOW))
                    )
                    stopMarker?.let { stopMarkers.add(it) }
                }

                // Si ya tenemos ubicación del driver, agregarla también
                driverLocation?.let { driverLoc ->
                    driverMarker = mMap?.addMarker(
                        MarkerOptions()
                            .position(driverLoc)
                            .title("Conductor")
                            .snippet("Ubicación actual del conductor")
                            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_CYAN))
                    )

                    Log.d("PassengerHome", "Conductor ubicado en: $driverLoc")

                    // Si el driver ya se movió del origen, actualizar la ruta inmediatamente
                    if (hasDriverMovedFromOrigin(driverLoc, origin)) {
                        Log.d("PassengerHome", "Driver ya se movió del origen, actualizando ruta")
                        // Usar post para evitar conflictos con el estado actual
                        Handler(Looper.getMainLooper()).post {
                            updateRouteFromDriverLocation()
                        }
                        return // La función updateRouteFromDriverLocation ya ajustará la cámara
                    }
                }

                // Ajustar cámara para mostrar toda la ruta incluyendo driver
                adjustCameraToShowRoute(origin, destination, stops)

                Log.d("PassengerHome", "Ruta mostrada correctamente")
            }
        } catch (e: Exception) {
            Log.e("PassengerHome", "Error mostrando ruta: ${e.message}")
            e.printStackTrace()
        }
    }

    private fun hasDriverMovedFromOrigin(driverLocation: LatLng, origin: LatLng): Boolean {
        val driverLocationObj = Location("driver").apply {
            latitude = driverLocation.latitude
            longitude = driverLocation.longitude
        }

        val originLocationObj = Location("origin").apply {
            latitude = origin.latitude
            longitude = origin.longitude
        }

        val distance = driverLocationObj.distanceTo(originLocationObj)

        // Si el driver está a más de 200m del origen, consideramos que ya se movió
        return distance > 200f
    }

    private fun adjustCameraToShowRoute(origin: LatLng, destination: LatLng, stops: List<LatLng>) {
        try {
            val builder = LatLngBounds.Builder()
            builder.include(origin)
            builder.include(destination)
            stops.forEach { builder.include(it) }

            // Incluir ubicación del driver si existe
            driverLocation?.let {
                builder.include(it)
                Log.d("PassengerHome", "Incluyendo ubicación driver en bounds: $it")
            }

            val bounds = builder.build()
            val padding = 200
            val cameraUpdate = CameraUpdateFactory.newLatLngBounds(bounds, padding)

            mMap?.animateCamera(cameraUpdate, 2000, object : GoogleMap.CancelableCallback {
                override fun onFinish() {
                    Log.d("PassengerHome", "Cámara ajustada exitosamente")
                }

                override fun onCancel() {
                    Log.d("PassengerHome", "Ajuste de cámara cancelado")
                }
            })
        } catch (e: Exception) {
            Log.w("PassengerHome", "Error ajustando cámara: ${e.message}")
            // Fallback: centrar en origen
            mMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(origin, 12f))
        }
    }


    private fun clearRouteMarkersOnly() {
        originMarker?.remove()
        destinationMarker?.remove()
        stopMarkers.forEach { it.remove() }
        stopMarkers.clear()

        // No remover el marcador del driver aquí
        originMarker = null
        destinationMarker = null
        stopMarkers.clear()
    }

    private fun updateDriverLocation(location: LatLng) {
        val previousDriverLocation = driverLocation
        driverLocation = location

        // Remover marcador anterior del conductor
        driverMarker?.remove()

        // Crear nuevo marcador del conductor
        driverMarker = mMap?.addMarker(
            MarkerOptions()
                .position(location)
                .title("Conductor")
                .snippet("Ubicación actual del conductor")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_CYAN))
        )

        Log.d("PassengerHome", "Marcador del conductor actualizado en: $location")

        // Si hay ruta activa y la ubicación del driver cambió significativamente, actualizar la ruta
        if (isInActiveTrip && destinationLocation != null) {
            if (previousDriverLocation == null) {
                // Primera vez que recibimos ubicación del conductor
                Log.d("PassengerHome", "Primera ubicación del conductor recibida")

                // Si el conductor ya se movió del origen, actualizar la ruta
                if (originLocation != null && hasDriverMovedFromOrigin(location, originLocation!!)) {
                    Log.d("PassengerHome", "Driver ya se movió del origen, actualizando ruta")
                    updateRouteFromDriverLocation()
                } else {
                    // Solo reajustar cámara para incluir al conductor
                    adjustCameraToShowRoute(originLocation ?: location, destinationLocation!!, stopLocations)
                }
            } else if (hasDriverMovedSignificantly(previousDriverLocation, location)) {
                Log.d("PassengerHome", "Driver se movió significativamente, actualizando ruta")
                updateRouteFromDriverLocation()
            } else {
                // Solo reajustar cámara sin redibujar ruta
                adjustCameraToShowDriverRoute(location, destinationLocation!!, stopLocations)
            }
        }
    }


    private fun hasDriverMovedSignificantly(
        previousLocation: LatLng,
        currentLocation: LatLng
    ): Boolean {
        val prevLocationObj = Location("previous").apply {
            latitude = previousLocation.latitude
            longitude = previousLocation.longitude
        }

        val currentLocationObj = Location("current").apply {
            latitude = currentLocation.latitude
            longitude = currentLocation.longitude
        }

        val distance = prevLocationObj.distanceTo(currentLocationObj)

        // Considerar movimiento significativo si es mayor a 100 metros
        return distance > 100f
    }


    private fun updateUIForActiveTrip() {
        // Ocultar el bottom sheet
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
        bottomSheetBehavior.isHideable = true

        // Ocultar el campo de destino y mostrar información del viaje
        binding.etDestination.isEnabled = false
        binding.etDestination.hint = "Viaje en curso..."

        // Remover el click listener para evitar que abra la búsqueda
        binding.etDestination.setOnClickListener(null)

        // Mostrar mensaje
        Toast.makeText(requireContext(), "Tienes un viaje activo", Toast.LENGTH_LONG).show()

        Log.d("PassengerHome", "UI actualizada para viaje activo")
    }

    private fun resetUIForNormalState() {
        Log.d("PassengerHome", "resetUIForNormalState - antes: isInActiveTrip=$isInActiveTrip")

        if (!isInActiveTrip) {
            Log.d("PassengerHome", "Ya está en estado normal, no hacer nada")
            return // Ya está en estado normal
        }

        // Limpiar listeners primero
        tripListener?.remove()
        tripListener = null

        isInActiveTrip = false
        currentTripId = null

        // Restaurar UI
        binding.etDestination.isEnabled = true
        binding.etDestination.hint = "¿A dónde quieres ir?"

        // Mostrar el bottom sheet nuevamente
        bottomSheetBehavior.isHideable = false
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED

        // Restablecer el flag del listener
        destinationListenerConfigured = false

        // Configurar nuevamente el click listener
        setupDestinationClickListener()

        // Limpiar mapa completamente
        mMap?.clear()
        clearAllMarkerReferences()
        routePolyline = null

        // Centrar en ubicación actual si está disponible
        currentLocation?.let { location ->
            Log.d("PassengerHome", "Centrando en ubicación actual tras reset")
            centerMapOnCurrentLocation(location)
        }

        Log.d("PassengerHome", "UI restablecida al estado normal")
    }

    private fun clearAllMarkerReferences() {
        originMarker = null
        destinationMarker = null
        driverMarker = null
        stopMarkers.clear()
        driverLocation = null
        originLocation = null
        destinationLocation = null
        stopLocations.clear()

        // Limpiar también las nuevas variables
        lastDriverLocationUpdate = null
    }


    private fun handleTripTerminated() {
        tripListener?.remove()
        tripListener = null

        val tripIdFinalizado = currentTripId

        Toast.makeText(requireContext(), "El viaje ha terminado", Toast.LENGTH_LONG).show()

        resetUIForNormalState()
        Log.d("PassengerHome", "Viaje terminado, UI restablecida")
        tripIdFinalizado?.let {
            val action = PassengerHomeFragmentDirections
                .actionPassengerHomeFragmentToRateDriverFragment(it)
            findNavController().navigate(action)
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

    private fun updateRouteFromDriverLocation() {
        val driverLoc = driverLocation ?: return
        val destination = destinationLocation ?: return

        Log.d("PassengerHome", "Actualizando ruta desde ubicación del driver: $driverLoc")

        // Filtrar paradas restantes basándose en la ubicación del driver
        val remainingStops = filterRemainingStopsForDriver(driverLoc, stopLocations)

        // Redibujar la ruta desde la ubicación actual del driver hasta el destino
        drawRouteFromDriverPosition(driverLoc, destination, remainingStops)
    }

    private fun drawRouteFromDriverPosition(
        origin: LatLng,
        destination: LatLng,
        stops: List<LatLng>
    ) {
        Log.d(
            "PassengerHome",
            "Dibujando ruta desde posición del driver: $origin hasta $destination con ${stops.size} paradas"
        )
        getDirectionsDataForDriverUpdate(origin, destination, stops)
    }

    private fun getDirectionsDataForDriverUpdate(
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

            Log.d("PassengerHome", "Solicitando direcciones actualizadas del driver: $urlString")

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

                    Log.d("PassengerHome", "Respuesta API status para driver update: $status")

                    if (status == "OK") {
                        val routes = jsonObject.getJSONArray("routes")

                        if (routes.length() > 0) {
                            val legs = routes.getJSONObject(0).getJSONArray("legs")
                            val path = ArrayList<LatLng>()

                            for (i in 0 until legs.length()) {
                                val steps = legs.getJSONObject(i).getJSONArray("steps")

                                for (j in 0 until steps.length()) {
                                    val points = steps.getJSONObject(j).getJSONObject("polyline")
                                        .getString("points")
                                    path.addAll(decodePoly(points))
                                }
                            }

                            Log.d(
                                "PassengerHome",
                                "Ruta del driver actualizada con ${path.size} puntos"
                            )

                            requireActivity().runOnUiThread {
                                displayUpdatedDriverRouteOnMap(path, origin, destination, waypoints)
                            }
                        }
                    } else {
                        Log.e(
                            "PassengerHome",
                            "Error en API Directions para driver update: $status"
                        )
                    }
                } catch (e: Exception) {
                    Log.e("PassengerHome", "Error en red para driver update: ${e.message}")
                    e.printStackTrace()
                }
            }
        } catch (e: Exception) {
            Log.e("PassengerHome", "Error configurando request para driver update: ${e.message}")
            e.printStackTrace()
        }
    }

    private fun displayUpdatedDriverRouteOnMap(
        path: List<LatLng>,
        driverLocation: LatLng,
        destination: LatLng,
        stops: List<LatLng>
    ) {
        try {
            Log.d(
                "PassengerHome",
                "Mostrando ruta actualizada del driver en mapa con ${path.size} puntos"
            )

            // Limpiar polyline anterior
            routePolyline?.remove()

            // Limpiar solo marcadores de ruta, pero mantener referencias
            clearRouteMarkersOnly()

            if (path.isNotEmpty()) {
                // Dibujar nueva polyline desde la ubicación del driver
                val polylineOptions = PolylineOptions()
                    .addAll(path)
                    .width(8f)
                    .color(Color.parseColor("#1976D2"))
                    .geodesic(true)

                routePolyline = mMap?.addPolyline(polylineOptions)

                // Marcador del conductor (ubicación actual)
                driverMarker = mMap?.addMarker(
                    MarkerOptions()
                        .position(driverLocation)
                        .title("Conductor")
                        .snippet("Ubicación actual del conductor")
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_CYAN))
                )

                // Marcador de destino
                destinationMarker = mMap?.addMarker(
                    MarkerOptions()
                        .position(destination)
                        .title("Destino")
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))
                )

                // Marcadores de paradas restantes
                stops.forEachIndexed { index, latLng ->
                    val stopMarker = mMap?.addMarker(
                        MarkerOptions()
                            .position(latLng)
                            .title("Parada ${index + 1}")
                            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_YELLOW))
                    )
                    stopMarker?.let { stopMarkers.add(it) }
                }

                // Solo mostrar origen si el driver no ha empezado el viaje
                if (shouldShowOriginMarker(driverLocation)) {
                    originMarker = mMap?.addMarker(
                        MarkerOptions()
                            .position(originLocation!!)
                            .title("Origen")
                            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN))
                    )
                }

                // Ajustar cámara para mostrar la ruta actualizada
                adjustCameraToShowDriverRoute(driverLocation, destination, stops)

                Log.d("PassengerHome", "Ruta del driver mostrada correctamente")
            }
        } catch (e: Exception) {
            Log.e("PassengerHome", "Error mostrando ruta actualizada del driver: ${e.message}")
            e.printStackTrace()
        }
    }

    private fun shouldShowOriginMarker(driverLocation: LatLng): Boolean {
        val origin = originLocation ?: return false

        // Calcular distancia entre driver y origen
        val driverLocationObj = Location("driver").apply {
            latitude = driverLocation.latitude
            longitude = driverLocation.longitude
        }

        val originLocationObj = Location("origin").apply {
            latitude = origin.latitude
            longitude = origin.longitude
        }

        val distance = driverLocationObj.distanceTo(originLocationObj)
        return distance > 500f
    }

    private fun adjustCameraToShowDriverRoute(
        driverLocation: LatLng,
        destination: LatLng,
        stops: List<LatLng>
    ) {
        try {
            val builder = LatLngBounds.Builder()
            builder.include(driverLocation)
            builder.include(destination)
            stops.forEach { builder.include(it) }

            // Incluir origen si está siendo mostrado
            if (shouldShowOriginMarker(driverLocation)) {
                originLocation?.let { builder.include(it) }
            }

            val bounds = builder.build()
            val padding = 200
            val cameraUpdate = CameraUpdateFactory.newLatLngBounds(bounds, padding)

            mMap?.animateCamera(cameraUpdate, 2000, object : GoogleMap.CancelableCallback {
                override fun onFinish() {
                    Log.d("PassengerHome", "Cámara ajustada exitosamente para ruta del driver")
                }

                override fun onCancel() {
                    Log.d("PassengerHome", "Ajuste de cámara cancelado para ruta del driver")
                }
            })
        } catch (e: Exception) {
            Log.w("PassengerHome", "Error ajustando cámara para ruta del driver: ${e.message}")
            mMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(driverLocation, 12f))
        }
    }

    private fun filterRemainingStopsForDriver(
        driverLocation: LatLng,
        allStops: List<LatLng>
    ): List<LatLng> {
        val remainingStops = mutableListOf<LatLng>()
        val driverLocationObj = Location("driver").apply {
            latitude = driverLocation.latitude
            longitude = driverLocation.longitude
        }

        for (stop in allStops) {
            val stopLocation = Location("stop").apply {
                latitude = stop.latitude
                longitude = stop.longitude
            }

            // Si la parada está a más de 300m del driver, considerarla como pendiente
            val distanceToStop = driverLocationObj.distanceTo(stopLocation)
            Log.d("PassengerHome", "Distancia del driver a parada: ${distanceToStop}m")

            if (distanceToStop > 300f) {
                remainingStops.add(stop)
            } else {
                Log.d("PassengerHome", "Parada ya visitada o muy cerca: ${distanceToStop}m")
            }
        }

        Log.d(
            "PassengerHome",
            "Paradas restantes para el driver: ${remainingStops.size} de ${allStops.size}"
        )
        return remainingStops
    }

    override fun onResume() {
        super.onResume()
        if (hasLocationPermission) {
            startLocationUpdates()
        }

        Log.d("PassengerHome", "onResume - isInActiveTrip: $isInActiveTrip")

        // Verificar viaje activo cada vez que se reanuda el fragment
        checkForActiveTrip()

        // Centrar apropiadamente después de verificar el viaje
        currentLocation?.let { location ->
            if (!isInActiveTrip) {
                Log.d(
                    "PassengerHome",
                    "onResume: No hay viaje activo, centrando en ubicación actual"
                )
                centerMapOnCurrentLocation(location)
            } else {
                Log.d("PassengerHome", "onResume: Hay viaje activo, manteniendo vista de ruta")
                // Si hay viaje activo y ya tenemos las ubicaciones, reajustar la cámara
                if (originLocation != null && destinationLocation != null) {
                    adjustCameraToShowRoute(originLocation!!, destinationLocation!!, stopLocations)
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        if (hasLocationPermission) {
            locationManager.removeUpdates(this)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        tripListener?.remove()
        networkExecutor.shutdown()
        if (hasLocationPermission) {
            locationManager.removeUpdates(this)
        }

        _binding = null
    }
}