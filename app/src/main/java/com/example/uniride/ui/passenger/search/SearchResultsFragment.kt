package com.example.uniride.ui.passenger.search

import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Geocoder
import android.os.Bundle
import android.os.StrictMode
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.uniride.R
import com.example.uniride.databinding.FragmentSearchResultsBinding
import com.example.uniride.domain.adapter.TripPassengerDetailAdapter
import com.example.uniride.domain.model.Car
import com.example.uniride.domain.model.PassengerRequest
import com.example.uniride.domain.model.RequestStatus
import com.example.uniride.domain.model.Route
import com.example.uniride.domain.model.Stop
import com.example.uniride.domain.model.TravelOption
import com.example.uniride.domain.model.Trip
import com.example.uniride.domain.model.User
import com.example.uniride.domain.model.front.TripInformation
import com.example.uniride.domain.model.front.TripPassengerDetail
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.Polyline
import com.google.android.gms.maps.model.PolylineOptions
import com.google.android.gms.tasks.Tasks
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.time.LocalDate

class SearchResultsFragment : Fragment(), OnMapReadyCallback {

    private var _binding: FragmentSearchResultsBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: TripPassengerDetailAdapter


    private lateinit var bottomSheetBehavior: BottomSheetBehavior<FrameLayout>

    // Mapa y componentes de ruta
    private var mMap: GoogleMap? = null
    private var originLocation: LatLng? = null
    private var destinationLocation: LatLng? = null
    private var routePolyline: Polyline? = null
    private lateinit var geocoder: Geocoder

    // Argumentos de navegación
    private var originAddress: String? = null
    private var destinationAddress: String? = null

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
        _binding = FragmentSearchResultsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val policy = StrictMode.ThreadPolicy.Builder().permitAll().build()
        StrictMode.setThreadPolicy(policy)

        geocoder = Geocoder(requireContext())

        arguments?.let { args ->
            originAddress = args.getString("origin")
            destinationAddress = args.getString("destination")

            binding.tvRoute.text = "$originAddress → $destinationAddress"
        }

        // Configurar el mapa
        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment?
        mapFragment?.getMapAsync(this)

        // Configurar RecyclerView
        binding.rvOptions.layoutManager = LinearLayoutManager(requireContext())

        // Buscar viajes reales
        val origin = originAddress ?: return
        val destination = destinationAddress ?: return

        fetchTripsForPassenger(origin, destination) { tripDetails ->
            if (tripDetails.isEmpty()) {
                Toast.makeText(requireContext(), "No se encontraron viajes.", Toast.LENGTH_SHORT).show()
                return@fetchTripsForPassenger
            }

            adapter = TripPassengerDetailAdapter(tripDetails) { selectedDetail ->
                showBottomSheet(selectedDetail)
            }


            binding.rvOptions.adapter = adapter
        }

        // Configurar el comportamiento del BottomSheet
        setupBottomSheet()

        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.btnChange.setOnClickListener {
            findNavController().navigate(R.id.searchInputFragment)
        }
    }



    private fun setupBottomSheet() {
        val bottomSheet = view?.findViewById<FrameLayout>(R.id.standard_bottom_sheet)
            ?: return

        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet)

        bottomSheetBehavior.apply {
            peekHeight = resources.getDimensionPixelSize(R.dimen.bottom_sheet_peek_height)
            state = BottomSheetBehavior.STATE_COLLAPSED
            isDraggable = true
            isHideable = false

            addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
                override fun onStateChanged(bottomSheet: View, newState: Int) {
                    when (newState) {
                        BottomSheetBehavior.STATE_COLLAPSED -> {
                            Log.d("BottomSheet", "Estado: Colapsado")
                        }
                        BottomSheetBehavior.STATE_EXPANDED -> {
                            Log.d("BottomSheet", "Estado: Expandido")
                        }
                    }
                }

                override fun onSlide(bottomSheet: View, slideOffset: Float) {
                }
            })
        }

        bottomSheet.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                if (bottomSheetBehavior.state == BottomSheetBehavior.STATE_COLLAPSED) {
                    bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
                    true
                } else false
            } else false
        }

        val grabber = view?.findViewById<View>(R.id.bottom_sheet_grabber)
        grabber?.setOnClickListener {
            if (bottomSheetBehavior.state == BottomSheetBehavior.STATE_COLLAPSED) {
                bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
            } else {
                bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
            }
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        mMap?.uiSettings?.apply {
            isZoomControlsEnabled = true
            isMyLocationButtonEnabled = true
            isZoomGesturesEnabled = true
            isScrollGesturesEnabled = true
            isRotateGesturesEnabled = true
        }

        if (ContextCompat.checkSelfPermission(requireContext(),android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        ) {
            mMap?.isMyLocationEnabled = true
        }

        // Convertir direcciones a coordenadas
        originAddress?.let { origin ->
            originLocation = findLocation(origin)

            destinationAddress?.let { destination ->
                destinationLocation = findLocation(destination)

                // Si tenemos ambas ubicaciones, dibujar la ruta
                if (originLocation != null && destinationLocation != null) {
                    drawRoute(originLocation!!, destinationLocation!!)

                    // Añadir marcadores
                    mMap?.addMarker(
                        MarkerOptions()
                            .position(originLocation!!)
                            .title("Origen: $origin")
                    )

                    mMap?.addMarker(
                        MarkerOptions()
                            .position(destinationLocation!!)
                            .title("Destino: $destination")
                    )
                } else {
                    Toast.makeText(requireContext(),"No se pudieron encontrar las ubicaciones",Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun findLocation(address: String): LatLng? {
        val addresses = geocoder.getFromLocationName(address, 2)
        if (addresses != null && addresses.isNotEmpty()) {
            val addr = addresses[0]
            return LatLng(addr.latitude, addr.longitude)
        }
        return null
    }

    private fun drawRoute(origin: LatLng, destination: LatLng) {
        try {
            val path = getDirectionsData(origin, destination)

            routePolyline?.remove()

            if (path.isNotEmpty()) {
                val polylineOptions = PolylineOptions()
                    .addAll(path)
                    .width(12f)
                    .color(Color.BLUE)
                    .geodesic(true)

                routePolyline = mMap?.addPolyline(polylineOptions)

                val builder = LatLngBounds.Builder()
                builder.include(origin)
                builder.include(destination)
                val bounds = builder.build()

                val padding = 100
                val cameraUpdate = CameraUpdateFactory.newLatLngBounds(bounds, padding)
                mMap?.animateCamera(cameraUpdate)
            } else {
                Toast.makeText(requireContext(), "No se pudo trazar la ruta", Toast.LENGTH_SHORT)
                    .show()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(requireContext(),"Error al obtener la ruta: ${e.message}",Toast.LENGTH_SHORT).show()
        }
    }

    private fun getDirectionsData(origin: LatLng, destination: LatLng): List<LatLng> {
        val path = ArrayList<LatLng>()
        try {
            // Construir URL para la API de Google Directions
            val urlString = "https://maps.googleapis.com/maps/api/directions/json?" +
                    "origin=${origin.latitude},${origin.longitude}" +
                    "&destination=${destination.latitude},${destination.longitude}" +
                    "&mode=driving" +
                    "&key=$DIRECTIONS_API_KEY"

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

                    if (legs.length() > 0) {
                        val steps = legs.getJSONObject(0).getJSONArray("steps")

                        for (i in 0 until steps.length()) {
                            val points =
                                steps.getJSONObject(i).getJSONObject("polyline").getString("points")
                            path.addAll(decodePoly(points))
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e("Route", "Error obteniendo ruta: ${e.message}")
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }


    //busca todos los trips que estén activos
    private fun fetchTripsForPassenger(
        originQuery: String,
        destinationQuery: String,
        onResult: (List<TripPassengerDetail>) -> Unit
    ) {
        val db = FirebaseFirestore.getInstance()
        val tripList = mutableListOf<TripPassengerDetail>()

        db.collection("Trips")
            .whereEqualTo("status", "PENDING")
            .get()
            .addOnSuccessListener { snapshot ->
                val trips = snapshot.toObjects(Trip::class.java)
                if (trips.isEmpty()) {
                    onResult(emptyList())
                    return@addOnSuccessListener
                }

                var completed = 0
                for (trip in trips) {
                    loadFullTripData(trip, originQuery, destinationQuery) { item ->
                        if (item != null) {
                            tripList.add(item)
                        }
                        completed++
                        if (completed == trips.size) {
                            onResult(tripList)
                        }
                    }
                }
            }
            .addOnFailureListener {
                onResult(emptyList())
            }
    }


    //filtra según el origen y destino
    private fun loadFullTripData(
        trip: Trip,
        originQuery: String,
        destinationQuery: String,
        onValid: (TripPassengerDetail?) -> Unit
    ) {
        val db = FirebaseFirestore.getInstance()

        db.collection("Routes").document(trip.idRoute)
            .get()
            .addOnSuccessListener { routeSnap ->
                val route = routeSnap.toObject(Route::class.java) ?: return@addOnSuccessListener onValid(null)

                val originTask = db.collection("Stops").document(route.idOrigin).get()
                val destinationTask = db.collection("Stops").document(route.idDestination).get()

                Tasks.whenAllSuccess<DocumentSnapshot>(originTask, destinationTask)
                    .addOnSuccessListener { docs ->
                        val originStop = docs[0].toObject(Stop::class.java)
                        val destinationStop = docs[1].toObject(Stop::class.java)

                        if (
                            originStop?.name == originQuery &&
                            destinationStop?.name == destinationQuery
                        ) {
                            // Buscar paradas intermedias
                            db.collection("StopsInRoute")
                                .whereEqualTo("idRoute", route.id)
                                .get()
                                .addOnSuccessListener { interSnap ->
                                    val stopIds = interSnap.mapNotNull { it.getString("idStop") }

                                    if (stopIds.isEmpty()) {
                                        buildTripPassengerDetail(trip, originStop, destinationStop, emptyList(), onValid)
                                        return@addOnSuccessListener
                                    }

                                    db.collection("Stops")
                                        .whereIn(FieldPath.documentId(), stopIds)
                                        .get()
                                        .addOnSuccessListener { interStopsSnap ->
                                            val intermediateStops = interStopsSnap.toObjects(Stop::class.java)
                                                .filter { it.name != originStop.name && it.name != destinationStop.name }
                                            buildTripPassengerDetail(trip, originStop, destinationStop, intermediateStops, onValid)
                                        }
                                }
                        } else {
                            onValid(null)
                        }
                    }
                    .addOnFailureListener {
                        onValid(null)
                    }
            }
            .addOnFailureListener {
                onValid(null)
            }
    }

    //carga
    private fun buildTripPassengerDetail(
        trip: Trip,
        originStop: Stop,
        destinationStop: Stop,
        intermediateStops: List<Stop>,
        onReady: (TripPassengerDetail?) -> Unit
    ) {
        val db = FirebaseFirestore.getInstance()

        db.collection("Cars").document(trip.idCar).get()
            .addOnSuccessListener { carSnap ->
                val car = carSnap.toObject(Car::class.java)

                db.collection("users").document(trip.idDriver).get()
                    .addOnSuccessListener { userSnap ->
                        val driver = userSnap.toObject(User::class.java) ?: return@addOnSuccessListener onReady(null)

                        val parsedDate = try {
                            LocalDate.parse(trip.date)
                        } catch (e: Exception) {
                            LocalDate.now()
                        }

                        val tripInfo = TripInformation(
                            carIcon = R.drawable.ic_car,
                            availableSeats = trip.places,
                            origin = originStop.name,
                            destination = destinationStop.name,
                            departureTime = trip.startTime,
                            travelDate = parsedDate,
                            price = trip.price,
                            carName = "${car?.brand} ${car?.model}",
                            carPlate = car?.licensePlate ?: "Sin placa"
                        )

                        val detail = TripPassengerDetail(
                            tripInformation = tripInfo,
                            driverUser = driver,
                            intermediateStops = intermediateStops,
                            tripId = trip.id ?: ""
                        )

                        onReady(detail)
                    }
            }
            .addOnFailureListener {
                onReady(null)
            }
    }


    //mostrar datos del bottom sheet
    private fun showBottomSheet(tripDetail: TripPassengerDetail) {
        val view = view ?: return
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED

        view.findViewById<RecyclerView>(R.id.rv_options)?.visibility = View.GONE
        val detailView = view.findViewById<View>(R.id.layout_travel_detail) ?: return
        detailView.visibility = View.VISIBLE

        val info = tripDetail.tripInformation
        val driver = tripDetail.driverUser

        detailView.findViewById<TextView>(R.id.tvDriverName)?.text = driver.username
        detailView.findViewById<TextView>(R.id.tvCarPlate)?.text = "Placas: ${info.carPlate}"
        detailView.findViewById<TextView>(R.id.tvDate)?.text = "Fecha: ${info.travelDate}"
        detailView.findViewById<TextView>(R.id.tvTime)?.text = "Salida: ${info.departureTime}"
        detailView.findViewById<TextView>(R.id.tvSeats)?.text = "Cupos disponibles: ${info.availableSeats}"
        detailView.findViewById<TextView>(R.id.tvRoute)?.text = "Ruta: ${info.origin} → ${info.destination}"
        detailView.findViewById<TextView>(R.id.tvPrice)?.text = "Precio: $${info.price}"

        val stopsText = tripDetail.intermediateStops
            .takeIf { it.isNotEmpty() }
            ?.joinToString(", ") { it.name }
            ?: "-"
        detailView.findViewById<TextView>(R.id.tvStops)?.text = "Paradas: $stopsText"

        // CAMBIO IMPORTANTE: Usar TravelDetailBottomSheet en lugar del botón inline
        detailView.findViewById<Button>(R.id.btnRequest)?.setOnClickListener {
            // Guardar la solicitud en Firebase
            savePassengerRequest(tripDetail.tripId) { success ->
                if (success) {
                    Toast.makeText(requireContext(), "Solicitaste el viaje a ${info.destination}", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(requireContext(), "Error al enviar solicitud", Toast.LENGTH_SHORT).show()
                }
            }
        }

        val btnClose = detailView.findViewById<ImageView>(R.id.btnClose)
        btnClose?.setOnClickListener {
            detailView.visibility = View.GONE
            view.findViewById<RecyclerView>(R.id.rv_options)?.visibility = View.VISIBLE
        }
    }

    private fun savePassengerRequest(tripId: String, onResult: (Boolean) -> Unit) {
        val db = FirebaseFirestore.getInstance()

        // Obtener el ID del usuario actual (necesitarás implementar esto según tu sistema de auth)
        val currentUserId = getCurrentUserId()

        if (currentUserId.isNullOrEmpty()) {
            onResult(false)
            return
        }

        // Verificar si ya existe una solicitud para este viaje y usuario
        db.collection("PassengerRequests")
            .whereEqualTo("idTrip", tripId)
            .whereEqualTo("idUser", currentUserId)
            .get()
            .addOnSuccessListener { snapshot ->
                if (snapshot.isEmpty) {
                    // No existe, crear nueva solicitud
                    val request = PassengerRequest(
                        idTrip = tripId,
                        idUser = currentUserId,
                        status = RequestStatus.PENDING
                    )

                    db.collection("PassengerRequests")
                        .add(request)
                        .addOnSuccessListener { docRef ->
                            // Actualizar el documento con su propio ID
                            docRef.update("id", docRef.id)
                                .addOnSuccessListener { onResult(true) }
                                .addOnFailureListener { onResult(false) }
                        }
                        .addOnFailureListener { onResult(false) }
                } else {
                    // Ya existe una solicitud
                    Toast.makeText(requireContext(), "Ya tienes una solicitud para este viaje", Toast.LENGTH_SHORT).show()
                    onResult(false)
                }
            }
            .addOnFailureListener { onResult(false) }
    }

    private fun getCurrentUserId(): String? {
        return FirebaseAuth.getInstance().currentUser?.uid
    }
}