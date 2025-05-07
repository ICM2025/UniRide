package com.example.uniride.ui.driver.home

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.location.Geocoder
import android.location.Location
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import com.example.uniride.R
import com.example.uniride.databinding.FragmentDriverHomeBinding
import com.example.uniride.ui.driver.publish.PublishTripFlowActivity
import com.example.uniride.utility.Config_permission
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.Polyline
import com.google.android.gms.maps.model.PolylineOptions
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.LatLngBounds
import android.graphics.Color
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import android.content.pm.PackageManager
import android.os.StrictMode
import android.util.Log

class DriverHomeFragment : Fragment(), OnMapReadyCallback {

    private var _binding: FragmentDriverHomeBinding? = null
    private val binding get() = _binding!!

    // Location utility
    private lateinit var locationPermissionManager: Config_permission

    // Route components
    private var mMap: GoogleMap? = null
    private var routePolyline: Polyline? = null
    private lateinit var geocoder: Geocoder
    private lateinit var sharedPreferences: SharedPreferences

    // Location variables
    private var originLocation: LatLng? = null
    private var destinationLocation: LatLng? = null
    private var stopLocations: MutableList<LatLng> = mutableListOf()

    // Activity result launcher for publish trip flow
    private lateinit var publishTripLauncher: ActivityResultLauncher<Intent>

    // Flag to track if route is displayed
    private var isRouteDisplayed = false

    // NUEVA VARIABLE: Flag para prevenir recarga de mapa por ubicación
    private var preventLocationCameraUpdate = false

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

        // Initialize SharedPreferences
        sharedPreferences = requireActivity().getSharedPreferences("route_data", Context.MODE_PRIVATE)

        // Initialize geocoder
        geocoder = Geocoder(requireContext())

        // Register the activity result launcher
        registerPublishTripLauncher()

        // Permitir operaciones de red en el hilo principal (solo para demo)
        val policy = StrictMode.ThreadPolicy.Builder().permitAll().build()
        StrictMode.setThreadPolicy(policy)

        // Initialize permission manager with map callback and location callback
        locationPermissionManager = Config_permission(
            fragment = this,
            mapReadyCallback = { googleMap ->
                // No hacemos nada adicional aquí, gestionamos todo en onMapReady
            },
            locationUpdateCallback = { location ->
                // MODIFICADO: Solo actualizar la cámara si no hay ruta activa
                if (!preventLocationCameraUpdate) {
                    onLocationUpdated(location)
                }

                // Comprobar si necesitamos volver a mostrar la ruta
                if (sharedPreferences.getBoolean("HAS_PUBLISHED_ROUTE", false) && !isRouteDisplayed) {
                    loadRouteAndDisplay()
                }
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
            publishTripLauncher.launch(intent)
        }
    }

    private fun registerPublishTripLauncher() {
        publishTripLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == android.app.Activity.RESULT_OK) {
                // Route was published, immediately load and display it
                loadRouteAndDisplay()

                // Update UI if needed to reflect that a route is active
                updateUIForActiveRoute()
            }
        }
    }

    private fun updateUIForActiveRoute() {
        // Update the UI to show that there's an active route
        // For example, change the "Publish Trip" button to "Edit Trip" or "Cancel Trip"
        if (sharedPreferences.getBoolean("HAS_PUBLISHED_ROUTE", false)) {
            binding.btnPublishTrip.text = "Editar Viaje"
            // Adicionalmente, activamos el flag para prevenir actualizaciones de cámara por ubicación
            preventLocationCameraUpdate = true
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        // Save map reference
        mMap = googleMap

        // Set map settings to ensure route display persists - similar to SearchResultsFragment
        googleMap.uiSettings.apply {
            isZoomControlsEnabled = true
            isMapToolbarEnabled = true
            isZoomGesturesEnabled = true
            isScrollGesturesEnabled = true
            isRotateGesturesEnabled = true
        }

        // MODIFICADO: No configuramos listeners que puedan borrar el mapa
        googleMap.setOnCameraMoveStartedListener(null)
        googleMap.setOnCameraIdleListener(null)

        // Delegate to the location permission manager
        locationPermissionManager.onMapReady(googleMap)

        // Check if there's a route to display
        if (sharedPreferences.getBoolean("HAS_PUBLISHED_ROUTE", false)) {
            // MODIFICADO: Cargar la ruta después de un pequeño retraso para asegurar que el mapa esté listo
            view?.postDelayed({
                loadRouteAndDisplay()
                updateUIForActiveRoute()
            }, 500)
        }
    }

    private fun loadRouteAndDisplay() {
        val originAddress = sharedPreferences.getString("ROUTE_ORIGIN", null)
        val destinationAddress = sharedPreferences.getString("ROUTE_DESTINATION", null)
        val stopsCount = sharedPreferences.getInt("ROUTE_STOPS_COUNT", 0)

        // If map is not ready yet, we'll try again later
        if (mMap == null) {
            return
        }

        // Clear previous stops and route
        stopLocations.clear()
        routePolyline?.remove()
        mMap?.clear()

        // Reset the displayed flag
        isRouteDisplayed = false

        // If we have origin and destination
        if (!originAddress.isNullOrEmpty() && !destinationAddress.isNullOrEmpty()) {
            // Convert addresses to coordinates
            originLocation = findLocation(originAddress)
            destinationLocation = findLocation(destinationAddress)

            // Load all stops
            for (i in 0 until stopsCount) {
                val stopAddress = sharedPreferences.getString("ROUTE_STOP_$i", null)
                if (!stopAddress.isNullOrEmpty()) {
                    val stopLocation = findLocation(stopAddress)
                    if (stopLocation != null) {
                        stopLocations.add(stopLocation)
                    }
                }
            }

            // Display route if we have origin and destination
            if (originLocation != null && destinationLocation != null) {
                drawRoute(originLocation!!, destinationLocation!!, stopLocations)
                // MODIFICADO: Activar el flag para prevenir que las actualizaciones de ubicación muevan la cámara
                preventLocationCameraUpdate = true
                // Set the flag that route is displayed
                isRouteDisplayed = true
            } else {
                Toast.makeText(
                    requireContext(),
                    "No se pudieron encontrar las ubicaciones",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

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

    private fun drawRoute(origin: LatLng, destination: LatLng, stops: List<LatLng>) {
        try {
            val path = getDirectionsData(origin, destination, stops)

            // Remove previous polyline if exists
            routePolyline?.remove()

            if (path.isNotEmpty()) {
                // Crear la polilínea de manera similar a SearchResultsFragment
                val polylineOptions = PolylineOptions()
                    .addAll(path)
                    .width(12f)
                    .color(Color.BLUE)
                    .geodesic(true)

                // Guardar referencia de la polilínea para poder acceder a ella más tarde
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

                // Add markers for stops
                stops.forEachIndexed { index, latLng ->
                    mMap?.addMarker(
                        MarkerOptions()
                            .position(latLng)
                            .title("Parada ${index + 1}")
                    )
                }

                // Create bounds for camera similar to SearchResultsFragment
                val builder = LatLngBounds.Builder()
                builder.include(origin)
                builder.include(destination)
                stops.forEach { builder.include(it) }

                try {
                    val bounds = builder.build()
                    val padding = 100
                    val cameraUpdate = CameraUpdateFactory.newLatLngBounds(bounds, padding)
                    // MODIFICADO: Usar moveCamera en lugar de animateCamera para evitar problemas con animaciones
                    mMap?.moveCamera(cameraUpdate)
                } catch (e: Exception) {
                    // Fallback in case bounds calculation fails
                    mMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(origin, 12f))
                    e.printStackTrace()
                }

                // Marcar que la ruta se visualiza correctamente
                isRouteDisplayed = true

                // AÑADIDO: Importante - prevenir que las actualizaciones de ubicación muevan la cámara
                preventLocationCameraUpdate = true
            } else {
                Toast.makeText(requireContext(), "No se pudo trazar la ruta", Toast.LENGTH_SHORT)
                    .show()
                isRouteDisplayed = false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(
                requireContext(),
                "Error al obtener la ruta: ${e.message}",
                Toast.LENGTH_SHORT
            ).show()
            isRouteDisplayed = false
        }
    }

    private fun getDirectionsData(
        origin: LatLng,
        destination: LatLng,
        waypoints: List<LatLng>
    ): List<LatLng> {
        val path = ArrayList<LatLng>()
        try {
            // Build URL for Google Directions API - similar to SearchResultsFragment approach
            var urlString = "https://maps.googleapis.com/maps/api/directions/json?" +
                    "origin=${origin.latitude},${origin.longitude}" +
                    "&destination=${destination.latitude},${destination.longitude}" +
                    "&mode=driving"

            // Add waypoints if any
            if (waypoints.isNotEmpty()) {
                urlString += "&waypoints="
                waypoints.forEachIndexed { index, point ->
                    urlString += "${point.latitude},${point.longitude}"
                    if (index < waypoints.size - 1) urlString += "|"
                }
            }

            urlString += "&key=$DIRECTIONS_API_KEY"

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

                    for (i in 0 until legs.length()) {
                        val steps = legs.getJSONObject(i).getJSONArray("steps")

                        for (j in 0 until steps.length()) {
                            val points =
                                steps.getJSONObject(j).getJSONObject("polyline").getString("points")
                            path.addAll(decodePoly(points))
                        }
                    }
                }
            } else {
                Log.e("DirectionsAPI", "Status no OK: $status")
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e("DirectionsAPI", "Error obteniendo ruta: ${e.message}")
        }
        return path
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

    // Handle specific driver actions when location is updated
    private fun onLocationUpdated(location: Location) {
        // MODIFICADO: No hacer nada si estamos previniendo actualizaciones de cámara por ubicación
        if (preventLocationCameraUpdate) {
            return
        }

        // Si no hay ruta activa o no se está mostrando, permitir que locationPermissionManager actualice la cámara
        if (sharedPreferences.getBoolean("HAS_PUBLISHED_ROUTE", false) && !isRouteDisplayed && mMap != null) {
            loadRouteAndDisplay()
        }
    }

    override fun onResume() {
        super.onResume()
        locationPermissionManager.onResume()

        // Update the UI if there's an active route
        if (sharedPreferences.getBoolean("HAS_PUBLISHED_ROUTE", false)) {
            updateUIForActiveRoute()
            // Make sure route is displayed when fragment resumes
            if (!isRouteDisplayed && mMap != null) {
                // MODIFICADO: Agregar un pequeño retraso para asegurar que el mapa esté listo
                view?.postDelayed({
                    loadRouteAndDisplay()
                }, 300)
            }
        }
    }

    override fun onPause() {
        super.onPause()
        locationPermissionManager.onPause()
        // MODIFICADO: No borrar la ruta al pausar el fragmento
        // No resetear isRouteDisplayed ni preventLocationCameraUpdate
    }

    // AÑADIDO: Método para manejar cuando se vuelve a este fragmento desde otro
    override fun onStart() {
        super.onStart()
        // Si hay una ruta publicada, asegurarse de que se muestre correctamente
        if (sharedPreferences.getBoolean("HAS_PUBLISHED_ROUTE", false)) {
            preventLocationCameraUpdate = true
            if (mMap != null && !isRouteDisplayed) {
                view?.postDelayed({
                    loadRouteAndDisplay()
                }, 300)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        // No eliminar la polilínea ni restablecer isRouteDisplayed aquí
        // Esto es crucial para que la ruta se mantenga al volver al fragmento
    }
}