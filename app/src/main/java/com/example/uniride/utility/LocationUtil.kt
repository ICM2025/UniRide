package com.example.uniride.utility

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Geocoder
import android.widget.Toast
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

class LocationUtil(private val context: Context) {
    private val geocoder = Geocoder(context, Locale.getDefault())

    // Para mantener referencias a los marcadores
    private val markers: MutableList<Marker> = mutableListOf()
    companion object {
        private const val POLYLINE_WIDTH = 12f
        private const val POLYLINE_COLOR = Color.RED
        private const val MAP_PADDING = 100

        fun getApiKeyFromManifest(context: Context): String {
            return try {
                val appInfo = context.packageManager.getApplicationInfo(
                    context.packageName,
                    PackageManager.GET_META_DATA
                )
                appInfo.metaData.getString("com.google.android.geo.API_KEY") ?: ""
            } catch (e: Exception) {
                ""
            }
        }

        // Variable estática para mantener la referencia a la polilínea entre llamadas
        private var routePolyline: Polyline? = null
        // Mantener una referencia al último mapa utilizado
        private var lastMap: GoogleMap? = null
    }

    /**
     * Obtiene una dirección a partir de coordenadas (geocodificación inversa)
     */
    fun findAddress(location: LatLng): String? {
        val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 2)
        if (addresses != null && addresses.isNotEmpty()) {
            val addr = addresses[0]
            return addr.getAddressLine(0)
        }
        return null
    }

    /**
     * Obtiene coordenadas a partir de una dirección (geocodificación)
     */
    fun findLocation(address: String): LatLng? {
        val addresses = geocoder.getFromLocationName(address, 2)
        if (addresses != null && addresses.isNotEmpty()) {
            val addr = addresses[0]
            return LatLng(addr.latitude, addr.longitude)
        }
        return null
    }

    /**
     * Dibuja una ruta entre dos puntos en el mapa
     */
    suspend fun drawRoute(
        map: GoogleMap,
        origin: LatLng,
        destination: LatLng,
        intermediateStops: List<LatLng> = emptyList()
    ): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val path = getDirectionsData(origin, destination, intermediateStops)

                withContext(Dispatchers.Main) {
                    // Almacenar referencia al mapa actual
                    lastMap = map

                    // Eliminar rutas y marcadores anteriores
                    routePolyline?.remove()
                    map.clear()

                    if (path.isNotEmpty()) {
                        // Crear opciones de polilínea
                        val polylineOptions = PolylineOptions()
                            .addAll(path)
                            .width(POLYLINE_WIDTH)
                            .color(POLYLINE_COLOR)
                            .geodesic(true)
                            .clickable(false)
                            .visible(true)

                        // Agregar polilínea al mapa y guardar referencia
                        routePolyline = map.addPolyline(polylineOptions)

                        // Forzar visibilidad de la polilínea
                        routePolyline?.isVisible = true

                        // Limpiar marcadores anteriores
                        markers.forEach { it.remove() }
                        markers.clear()

                        // Agregar marcadores para origen y destino
                        markers.add(map.addMarker(
                            MarkerOptions()
                                .position(origin)
                                .title("Origen")
                                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN))
                        )!!)

                        markers.add(map.addMarker(
                            MarkerOptions()
                                .position(destination)
                                .title("Destino")
                                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))
                        )!!)

                        // Agregar marcadores para paradas intermedias
                        intermediateStops.forEachIndexed { index, latLng ->
                            markers.add(map.addMarker(
                                MarkerOptions()
                                    .position(latLng)
                                    .title("Parada ${index + 1}")
                                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_YELLOW))
                            )!!)
                        }

                        // Crear límites para incluir todos los puntos
                        val boundsBuilder = LatLngBounds.Builder()
                        boundsBuilder.include(origin)
                        boundsBuilder.include(destination)
                        intermediateStops.forEach { boundsBuilder.include(it) }
                        val bounds = boundsBuilder.build()

                        // Animar cámara para mostrar toda la ruta
                        val cameraUpdate = CameraUpdateFactory.newLatLngBounds(bounds, MAP_PADDING)
                        map.animateCamera(cameraUpdate)

                        // Pequeña pausa para asegurar que la ruta se dibuje completamente
                        delay(200)

                        // Verificar que la polilínea esté visible
                        if (routePolyline?.isVisible == false) {
                            routePolyline?.isVisible = true
                        }

                        return@withContext true
                    } else {
                        Toast.makeText(context, "No se pudo trazar la ruta", Toast.LENGTH_SHORT).show()
                        return@withContext false
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        context,
                        "Error al obtener la ruta: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                return@withContext false
            }
        }
    }

    /**
     * Dibuja una ruta simulada cuando no hay una clave de API disponible
     */
    fun drawMockRoute(
        map: GoogleMap,
        origin: LatLng,
        destination: LatLng,
        intermediateStops: List<LatLng> = emptyList()
    ) {
        // Almacenar referencia al mapa actual
        lastMap = map

        // Eliminar rutas y marcadores anteriores
        routePolyline?.remove()
        map.clear()

        // Limpiar marcadores anteriores
        markers.forEach { it.remove() }
        markers.clear()

        // Agregar marcadores para origen y destino
        markers.add(map.addMarker(
            MarkerOptions()
                .position(origin)
                .title("Origen")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN))
        )!!)

        markers.add(map.addMarker(
            MarkerOptions()
                .position(destination)
                .title("Destino")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))
        )!!)

        // Agregar marcadores para paradas intermedias
        intermediateStops.forEachIndexed { index, latLng ->
            markers.add(map.addMarker(
                MarkerOptions()
                    .position(latLng)
                    .title("Parada ${index + 1}")
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_YELLOW))
            )!!)
        }

        // Crear polilínea entre todos los puntos
        val polylineOptions = PolylineOptions()
            .width(POLYLINE_WIDTH)
            .color(POLYLINE_COLOR)
            .geodesic(true)
            .clickable(false)
            .visible(true)

        // Agregar puntos en orden
        polylineOptions.add(origin)
        intermediateStops.forEach { polylineOptions.add(it) }
        polylineOptions.add(destination)

        // Agregar polilínea al mapa y guardar referencia
        routePolyline = map.addPolyline(polylineOptions)
        routePolyline?.isVisible = true

        // Crear límites y mover cámara
        val boundsBuilder = LatLngBounds.Builder()
            .include(origin)
            .include(destination)
        intermediateStops.forEach { boundsBuilder.include(it) }

        val bounds = boundsBuilder.build()
        val cameraUpdate = CameraUpdateFactory.newLatLngBounds(bounds, MAP_PADDING)
        map.animateCamera(cameraUpdate)
    }

    /**
     * Obtiene datos de dirección utilizando la API de Google Directions
     */
    private fun getDirectionsData(
        origin: LatLng,
        destination: LatLng,
        waypoints: List<LatLng> = emptyList()
    ): List<LatLng> {
        val path = ArrayList<LatLng>()
        try {
            // Construir URL para la API de Google Directions
            var urlString = "https://maps.googleapis.com/maps/api/directions/json?" +
                    "origin=${origin.latitude},${origin.longitude}" +
                    "&destination=${destination.latitude},${destination.longitude}" +
                    "&mode=driving"

            // Agregar waypoints si existen
            if (waypoints.isNotEmpty()) {
                urlString += "&waypoints="
                waypoints.forEachIndexed { index, point ->
                    urlString += "${point.latitude},${point.longitude}"
                    if (index < waypoints.size - 1) urlString += "|"
                }
            }
            val DIRECTIONS_API_KEY = getApiKeyFromManifest(context)
            urlString += "&key=$DIRECTIONS_API_KEY"

            val url = URL(urlString)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"

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
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return path
    }

    /**
     * Decodifica el formato polilineal de Google Maps
     */
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
}

