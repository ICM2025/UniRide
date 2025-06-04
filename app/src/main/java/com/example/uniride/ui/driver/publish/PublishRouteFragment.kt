package com.example.uniride.ui.driver.publish

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.location.Geocoder
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognizerIntent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.uniride.R
import com.example.uniride.databinding.FragmentPublishRouteBinding
import com.example.uniride.domain.model.Car
import com.example.uniride.domain.model.Route
import com.example.uniride.domain.model.Stop
import com.example.uniride.domain.model.StopInRoute
import com.example.uniride.domain.model.Trip
import com.example.uniride.domain.model.TripStatus
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class PublishRouteFragment : Fragment() {

    private var _binding: FragmentPublishRouteBinding? = null
    private val binding get() = _binding!!

    //private lateinit var vehicleList: List<Vehicle>
    private val stopsList = mutableListOf<String>()
    private lateinit var sharedPreferences: SharedPreferences

    // Variables para fecha y hora
    private var selectedDate: Calendar = Calendar.getInstance()
    private var selectedTime: Calendar = Calendar.getInstance()
    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

    // Variables para edición
    private var isEditing = false
    private var editingTripId: String? = null

    // Enum para saber qué campo de texto está activo para el reconocimiento de voz
    private enum class EditTextField {
        ORIGIN, DESTINATION
    }

    private var activeField: EditTextField = EditTextField.ORIGIN

    // ActivityResultLauncher para la búsqueda por voz
    private val speechRecognizerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            val spokenText = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.get(0) ?: ""
            when (activeField) {
                EditTextField.ORIGIN -> binding.inputOrigin.setText(spokenText)
                EditTextField.DESTINATION -> binding.inputDestination.setText(spokenText)
            }
        }
    }

    //Firebase
    private val db = FirebaseFirestore.getInstance()
    private val uid = FirebaseAuth.getInstance().currentUser?.uid

    //lista de carros del usuario
    private lateinit var vehicleList: List<Car>
    //carro seleccionado
    private var selectedCar: Car? = null





    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPublishRouteBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        sharedPreferences = requireActivity().getSharedPreferences("route_data", Context.MODE_PRIVATE)

        setupVehicleSpinner()
        //si se acaba de agregar un vehículo lo preselecciona
        parentFragmentManager.setFragmentResultListener("vehicle_added", viewLifecycleOwner) { _, bundle ->
            val newVehicleId = bundle.getString("newVehicleId")
            if (newVehicleId != null) {
                setupVehicleSpinner(preselectVehicleId = newVehicleId)
            } else {
                //si no hay vehículo recién cargado
                setupVehicleSpinner()
            }
        }

        checkEditMode()
        setupDateTimePickers()
        setupContinueButton()
        setupVoiceRecognition()

        initStopFields()
        binding.btnAddStop.setOnClickListener {
            addNewStopField()
        }
    }

    private fun setupDateTimePickers() {
        // Configurar valores iniciales
        updateDateTimeDisplay()

        // Configurar selector de fecha
        binding.inputDate.setOnClickListener {
            showDatePicker()
        }

        // Configurar selector de hora
        binding.inputTime.setOnClickListener {
            showTimePicker()
        }

        // Hacer que los campos no sean editables directamente
        binding.inputDate.isFocusable = false
        binding.inputDate.isClickable = true
        binding.inputTime.isFocusable = false
        binding.inputTime.isClickable = true
    }

    private fun showDatePicker() {
        val datePickerDialog = DatePickerDialog(
            requireContext(),
            { _, year, month, dayOfMonth ->
                selectedDate.set(Calendar.YEAR, year)
                selectedDate.set(Calendar.MONTH, month)
                selectedDate.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                updateDateDisplay()
            },
            selectedDate.get(Calendar.YEAR),
            selectedDate.get(Calendar.MONTH),
            selectedDate.get(Calendar.DAY_OF_MONTH)
        )

        // No permitir fechas pasadas
        datePickerDialog.datePicker.minDate = System.currentTimeMillis()
        datePickerDialog.show()
    }

    private fun showTimePicker() {
        val timePickerDialog = TimePickerDialog(
            requireContext(),
            { _, hourOfDay, minute ->
                selectedTime.set(Calendar.HOUR_OF_DAY, hourOfDay)
                selectedTime.set(Calendar.MINUTE, minute)
                updateTimeDisplay()
            },
            selectedTime.get(Calendar.HOUR_OF_DAY),
            selectedTime.get(Calendar.MINUTE),
            true // Formato 24 horas
        )
        timePickerDialog.show()
    }

    private fun updateDateTimeDisplay() {
        updateDateDisplay()
        updateTimeDisplay()
    }

    private fun updateDateDisplay() {
        binding.inputDate.setText(dateFormat.format(selectedDate.time))
    }

    private fun updateTimeDisplay() {
        binding.inputTime.setText(timeFormat.format(selectedTime.time))
    }

    private fun checkEditMode() {
        val intent = requireActivity().intent
        isEditing = intent.getBooleanExtra("IS_EDITING", false)
        editingTripId = intent.getStringExtra("TRIP_ID")

        if (isEditing && !editingTripId.isNullOrEmpty()) {
            loadTripDataFromFirebase()
            binding.btnContinue.text = "Actualizar viaje"
        }
    }

    private fun loadTripDataFromFirebase() {
        editingTripId?.let { tripId ->
            db.collection("Trips").document(tripId)
                .get()
                .addOnSuccessListener { tripDoc ->
                    if (tripDoc.exists()) {
                        val trip = tripDoc.toObject(Trip::class.java)
                        trip?.let { loadTripDetails(it) }
                    } else {
                        Toast.makeText(requireContext(), "Viaje no encontrado", Toast.LENGTH_SHORT).show()
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(requireContext(), "Error al cargar datos del viaje", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun loadTripDetails(trip: Trip) {
        // Cargar datos básicos del viaje
        binding.inputPrice.setText(trip.price.toString())
        binding.inputSeats.setText(trip.places.toString())
        binding.inputDescription.setText(trip.description)

        // Cargar fecha y hora
        try {
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

            selectedDate.time = dateFormat.parse(trip.date) ?: Calendar.getInstance().time
            selectedTime.time = timeFormat.parse(trip.startTime) ?: Calendar.getInstance().time

            updateDateTimeDisplay()
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Error al cargar fecha/hora", Toast.LENGTH_SHORT).show()
        }

        // Cargar vehículo seleccionado
        loadVehicleSelection(trip.idCar)

        // Cargar ruta (origen, destino y paradas)
        loadRouteData(trip.idRoute)
    }

    private fun loadVehicleSelection(carId: String) {
        // Esperar a que se carguen los vehículos y luego seleccionar
        loadVehiclesFromFirestore { vehicles ->
            vehicleList = vehicles
            setupVehicleSpinnerWithSelection(carId)
        }
    }

    private fun setupVehicleSpinnerWithSelection(preselectCarId: String? = null) {
        val vehicleNames = mutableListOf("Seleccionar un vehículo")
        if (vehicleList.isNotEmpty()) {
            vehicleNames.addAll(vehicleList.map { "${it.brand} ${it.model} (${it.licensePlate})" })
        }
        vehicleNames.add("Agregar vehículo")

        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, vehicleNames)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerCar.adapter = adapter

        // Preseleccionar por ID del carro
        if (!preselectCarId.isNullOrEmpty()) {
            val index = vehicleList.indexOfFirst { it.id == preselectCarId }
            if (index != -1) {
                binding.spinnerCar.setSelection(index + 1)
                selectedCar = vehicleList[index]
            }
        }

        binding.spinnerCar.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                val selected = parent.getItemAtPosition(position).toString()
                when (selected) {
                    "Agregar vehículo" -> findNavController()
                        .navigate(R.id.action_publishRouteFragment_to_addVehicleFragment)
                    "Seleccionar un vehículo" -> selectedCar = null
                    else -> {
                        selectedCar = if (position > 0 && position <= vehicleList.size) {
                            vehicleList[position - 1]
                        } else null
                    }
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
    }

    private fun loadRouteData(routeId: String) {
        // Primero obtener la ruta
        db.collection("Routes").document(routeId)
            .get()
            .addOnSuccessListener { routeDoc ->
                if (routeDoc.exists()) {
                    val route = routeDoc.toObject(Route::class.java)
                    route?.let {
                        // Cargar paradas ordenadas por posición
                        db.collection("StopsInRoute")
                            .whereEqualTo("idRoute", route.id)
                            .orderBy("position")
                            .get()
                            .addOnSuccessListener { stopsInRoute ->
                                val stopIds = stopsInRoute.documents.mapNotNull { doc ->
                                    doc.toObject(StopInRoute::class.java)?.idStop
                                }

                                // Verificar que tenemos paradas
                                if (stopIds.isNotEmpty()) {
                                    loadStopNames(stopIds)
                                } else {
                                    Toast.makeText(requireContext(), "No se encontraron paradas para esta ruta", Toast.LENGTH_SHORT).show()
                                }
                            }
                            .addOnFailureListener {
                                Toast.makeText(requireContext(), "Error al cargar paradas de la ruta", Toast.LENGTH_SHORT).show()
                            }
                    }
                } else {
                    Toast.makeText(requireContext(), "Ruta no encontrada", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Error al cargar ruta", Toast.LENGTH_SHORT).show()
            }
    }

    private fun loadIntermediateStopsInUI(intermediateStops: List<String>) {
        // Limpiar paradas existentes
        binding.stopsContainer.removeAllViews()

        // Si hay paradas intermedias, crearlas
        if (intermediateStops.isNotEmpty()) {
            intermediateStops.forEach { stopName ->
                addNewStopField()
                val lastStopView = binding.stopsContainer.getChildAt(binding.stopsContainer.childCount - 1)
                val stopEditText = lastStopView.findViewById<EditText>(R.id.et_stop)
                stopEditText.setText(stopName)
            }
        }

        // Siempre agregar al menos un campo vacío para nuevas paradas
        addNewStopField()
    }

    private fun loadStopNames(stopIds: List<String>) {
        if (stopIds.isEmpty()) return

        val tasks = stopIds.map { stopId ->
            db.collection("Stops").document(stopId).get()
        }

        Tasks.whenAllComplete(tasks)
            .addOnSuccessListener { results ->
                val stops = results.mapIndexedNotNull { index, task ->
                    if (task.isSuccessful) {
                        val stopDoc = task.result as? com.google.firebase.firestore.DocumentSnapshot
                        stopDoc?.toObject(Stop::class.java)?.let { stop ->
                            stop.copy(id = stopDoc.id)
                        }
                    } else null
                }

                // Asegurar que tenemos al menos origen y destino
                if (stops.size >= 2) {
                    // El primer elemento es origen, el último es destino
                    val origin = stops.first()
                    val destination = stops.last()

                    // Poblar los campos de origen y destino
                    binding.inputOrigin.setText(origin.name)
                    binding.inputDestination.setText(destination.name)

                    // Las paradas intermedias (si las hay)
                    val intermediateStops = stops.drop(1).dropLast(1)
                    loadIntermediateStopsInUI(intermediateStops.map { it.name })
                }
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Error al cargar nombres de paradas", Toast.LENGTH_SHORT).show()
            }
    }

    private fun updateTripInFirebase(origin: String, destination: String, stops: List<String>) {
        editingTripId?.let { tripId ->

            // Primero crear/obtener las paradas
            getOrCreateStopByName(origin) { originStop ->
                if (originStop == null) return@getOrCreateStopByName

                getOrCreateStopByName(destination) { destinationStop ->
                    if (destinationStop == null) return@getOrCreateStopByName

                    getOrCreateIntermediateStops(stops) { intermediateStops ->

                        // Obtener el viaje actual para obtener su routeId
                        db.collection("Trips").document(tripId)
                            .get()
                            .addOnSuccessListener { tripDoc ->
                                val currentTrip = tripDoc.toObject(Trip::class.java)
                                if (currentTrip != null) {
                                    updateRouteAndTrip(currentTrip, originStop, destinationStop, intermediateStops)
                                }
                            }
                    }
                }
            }
        }
    }

    private fun updateRouteAndTrip(
        currentTrip: Trip,
        originStop: Stop,
        destinationStop: Stop,
        intermediateStops: List<Stop>
    ) {
        val routeId = currentTrip.idRoute

        // Actualizar la ruta
        val routeUpdates = mapOf(
            "idOrigin" to (originStop.id ?: ""),
            "idDestination" to (destinationStop.id ?: "")
        )

        db.collection("Routes").document(routeId)
            .update(routeUpdates)
            .addOnSuccessListener {
                // Eliminar paradas antiguas de la ruta
                db.collection("StopsInRoute")
                    .whereEqualTo("idRoute", routeId)
                    .get()
                    .addOnSuccessListener { oldStops ->
                        // Eliminar paradas antiguas
                        val deleteTasks = oldStops.documents.map { it.reference.delete() }

                        Tasks.whenAllComplete(deleteTasks)
                            .addOnSuccessListener {
                                // Agregar nuevas paradas
                                addNewStopsToRoute(routeId, originStop, destinationStop, intermediateStops) {
                                    updateTripDetails(currentTrip.id.toString())
                                }
                            }
                    }
            }
    }

    private fun addNewStopsToRoute(
        routeId: String,
        originStop: Stop,
        destinationStop: Stop,
        intermediateStops: List<Stop>,
        onComplete: () -> Unit
    ) {
        val allStopsOrdered = listOf(originStop) + intermediateStops + listOf(destinationStop)
        val stopsInRouteRef = db.collection("StopsInRoute")

        val tasks = allStopsOrdered.mapIndexed { index, stop ->
            val stopInRoute = StopInRoute(
                idRoute = routeId,
                idStop = stop.id ?: "",
                position = index
            )
            stopsInRouteRef.add(stopInRoute)
        }

        Tasks.whenAllComplete(tasks)
            .addOnSuccessListener {
                onComplete()
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Error al actualizar paradas", Toast.LENGTH_SHORT).show()
            }
    }

    private fun updateTripDetails(tripId: String) {
        val price = binding.inputPrice.text.toString().toDoubleOrNull() ?: 0.0
        val places = binding.inputSeats.text.toString().toIntOrNull() ?: 1

        val tripUpdates = mapOf(
            "idCar" to (selectedCar?.id ?: ""),
            "description" to binding.inputDescription.text.toString().trim(),
            "price" to price,
            "places" to places,
            "date" to SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(selectedDate.time),
            "startTime" to SimpleDateFormat("HH:mm", Locale.getDefault()).format(selectedTime.time),
            "updatedAt" to Date()
        )

        db.collection("Trips").document(tripId)
            .update(tripUpdates)
            .addOnSuccessListener {
                showTripUpdateSuccessDialog()
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Error al actualizar el viaje", Toast.LENGTH_SHORT).show()
            }
    }

    private fun showTripUpdateSuccessDialog() {
        val activity = requireActivity()
        val dialogView = layoutInflater.inflate(R.layout.dialog_success_request, null)

        dialogView.findViewById<TextView>(R.id.tv_success).text = "¡Viaje actualizado!"
        dialogView.findViewById<TextView>(R.id.tv_secondary).text = "Los cambios se han guardado exitosamente"

        val dialog = AlertDialog.Builder(activity)
            .setView(dialogView)
            .create()

        dialog.window?.setDimAmount(0.75f)
        dialog.show()

        Handler(Looper.getMainLooper()).postDelayed({
            dialog.dismiss()
            activity.setResult(Activity.RESULT_OK)
            activity.finish()
        }, 1500)
    }

    private fun createNewTrip(origin: String, destination: String, stops: List<String>) {
        getOrCreateStopByName(origin) { originStop ->
            if (originStop == null) return@getOrCreateStopByName

            getOrCreateStopByName(destination) { destinationStop ->
                if (destinationStop == null) return@getOrCreateStopByName

                getOrCreateIntermediateStops(stops) { intermediateStops ->
                    createRouteWithStops(originStop, destinationStop, intermediateStops,
                        onSuccess = { routeId ->
                            createTrip(routeId)
                        },
                        onError = {
                            Toast.makeText(requireContext(), "No se pudo crear la ruta", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }
        }
    }

    private fun setupVoiceRecognition() {
        // Configurar botones de reconocimiento de voz
        binding.btnMicOrigin.setOnClickListener {
            activeField = EditTextField.ORIGIN
            startVoiceRecognition()
        }

        binding.btnMicDestination.setOnClickListener {
            activeField = EditTextField.DESTINATION
            startVoiceRecognition()
        }

        binding.inputOrigin.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                activeField = EditTextField.ORIGIN
            }
        }

        binding.inputDestination.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                activeField = EditTextField.DESTINATION
            }
        }
    }

    // Inicia la actividad de reconocimiento de voz
    private fun startVoiceRecognition() {
        if (ActivityCompat.checkSelfPermission(requireContext(),Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(requireActivity(),arrayOf(Manifest.permission.RECORD_AUDIO),
                VOICE_PERMISSION_REQUEST_CODE
            )
            return
        }

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Habla ahora...")
        }

        try {
            speechRecognizerLauncher.launch(intent)
        } catch (e: Exception) {
            Toast.makeText(
                requireContext(),
                "Tu dispositivo no soporta reconocimiento de voz",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    //elegir uno de los vehículos registrados del conductor
    private fun setupVehicleSpinner(preselectVehicleId: String? = null) {

        loadVehiclesFromFirestore { vehicles ->
            vehicleList = vehicles

            // Crear lista con opción inicial "Agregar vehículo"
            val vehicleNames = mutableListOf("Seleccionar un vehículo")
            if (vehicles.isNotEmpty()) {
                //datos que se agregan para cada carro
                vehicleNames.addAll(vehicles.map { "${it.brand} ${it.model} (${it.licensePlate})" })
            }
            vehicleNames.add("Agregar vehículo")

            val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, vehicleNames)
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            binding.spinnerCar.adapter = adapter

            // Si se pasó un id de vehículo se selecciona de una vez
            if (preselectVehicleId != null) {
                val index = vehicles.indexOfFirst { it.id == preselectVehicleId }
                if (index != -1) {
                    // +1 por la opción "Seleccionar un vehículo" en la posición 0
                    binding.spinnerCar.setSelection(index + 1)
                }
            }

            //  al seleccionar una opción del spinner
            binding.spinnerCar.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                    val selected = parent.getItemAtPosition(position).toString()

                    when (selected) {
                        "Agregar vehículo" -> findNavController()
                            .navigate(R.id.action_publishRouteFragment_to_addVehicleFragment)
                        "Seleccionar un vehículo" -> {
                            // No se hace nada, simplemente es la opción inicial
                        }
                        else -> {
                            // Vehículo válido seleccionado
                            selectedCar = if (
                                binding.spinnerCar.selectedItemPosition > 0 &&
                                binding.spinnerCar.selectedItemPosition <= vehicleList.size
                            ) {
                                vehicleList[binding.spinnerCar.selectedItemPosition - 1]
                            } else null
                        }
                    }
                }

                override fun onNothingSelected(parent: AdapterView<*>) {}
            }
        }
    }



    private fun setupContinueButton() {
        binding.btnContinue.setOnClickListener {
            if (!validateTripInputs()) return@setOnClickListener

            val originAddress = binding.inputOrigin.text.toString().trim()
            val destinationAddress = binding.inputDestination.text.toString().trim()

            // Recopilar paradas intermedias
            stopsList.clear()
            for (i in 0 until binding.stopsContainer.childCount) {
                val stopView = binding.stopsContainer.getChildAt(i)
                val stopEditText = stopView.findViewById<EditText>(R.id.et_stop)
                val stopText = stopEditText.text.toString().trim()
                if (stopText.isNotEmpty()) {
                    stopsList.add(stopText)
                }
            }

            if (isEditing && !editingTripId.isNullOrEmpty()) {
                // Actualizar viaje existente en Firebase
                updateTripInFirebase(originAddress, destinationAddress, stopsList)
            } else {
                // Crear nuevo viaje (código existente)
                createNewTrip(originAddress, destinationAddress, stopsList)
            }
        }
    }

    //carros registrados por el conductor
    private fun loadVehiclesFromFirestore(onComplete: (List<Car>) -> Unit) {
        if (uid == null) return

        db.collection("Cars")
            .whereEqualTo("idDriver", uid)
            .get()
            .addOnSuccessListener { result ->
                val vehicles = result.documents.mapNotNull { it.toObject(Car::class.java) }
                onComplete(vehicles)
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Error al cargar vehículos", Toast.LENGTH_SHORT).show()
                onComplete(emptyList())
            }
    }


    private fun initStopFields() {
        // Solo agregar un campo si no estamos editando o si no hay paradas cargadas
        if (!isEditing || binding.stopsContainer.childCount == 0) {
            addNewStopField()
        }
    }

    private fun addNewStopField() {
        val stopView =
            layoutInflater.inflate(R.layout.item_stop_field, binding.stopsContainer, false)

        val btnDelete = stopView.findViewById<ImageButton>(R.id.btn_delete)
        btnDelete.setOnClickListener {
            binding.stopsContainer.removeView(stopView)
        }
        binding.stopsContainer.addView(stopView)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            VOICE_PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    startVoiceRecognition()
                } else {
                    Toast.makeText(requireContext(),"Permiso de micrófono denegado",Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val VOICE_PERMISSION_REQUEST_CODE = 101
    }





    //validar campos del viaje antes de publicarlo
    private fun validateTripInputs(): Boolean {
        val originAddress = binding.inputOrigin.text.toString().trim()
        val destinationAddress = binding.inputDestination.text.toString().trim()
        val selectedDateStr = binding.inputDate.text.toString().trim()
        val selectedTimeStr = binding.inputTime.text.toString().trim()

        if (originAddress.isEmpty() || destinationAddress.isEmpty()) {
            Toast.makeText(requireContext(), "Por favor completa origen y destino", Toast.LENGTH_SHORT).show()
            return false
        }

        if (selectedCar == null) {
            Toast.makeText(requireContext(), "Debes seleccionar un vehículo", Toast.LENGTH_SHORT).show()
            return false
        }

        if (selectedDateStr.isEmpty() || selectedTimeStr.isEmpty()) {
            Toast.makeText(requireContext(), "Por favor selecciona fecha y hora", Toast.LENGTH_SHORT).show()
            return false
        }

        val selectedDateTime = Calendar.getInstance().apply {
            set(Calendar.YEAR, selectedDate.get(Calendar.YEAR))
            set(Calendar.MONTH, selectedDate.get(Calendar.MONTH))
            set(Calendar.DAY_OF_MONTH, selectedDate.get(Calendar.DAY_OF_MONTH))
            set(Calendar.HOUR_OF_DAY, selectedTime.get(Calendar.HOUR_OF_DAY))
            set(Calendar.MINUTE, selectedTime.get(Calendar.MINUTE))
        }

        if (selectedDateTime.timeInMillis < System.currentTimeMillis()) {
            Toast.makeText(requireContext(), "No puedes programar un viaje en el pasado", Toast.LENGTH_SHORT).show()
            return false
        }

        return true
    }

    //geodificar y subir a firestore una parada en específico
    private fun getOrCreateStopByName(name: String, onResult: (Stop?) -> Unit) {
        val stopsRef = db.collection("Stops")

        stopsRef.whereEqualTo("name", name)
            .limit(1)
            .get()
            .addOnSuccessListener { documents ->
                if (!documents.isEmpty) {
                    val existingStop = documents.first().toObject(Stop::class.java).copy(
                        id = documents.first().id
                    )
                    onResult(existingStop)
                } else {
                    //si la parada no está en la base de datos la crea usando geocoder
                    val geocoder = Geocoder(requireContext(), Locale.getDefault())
                    val addresses = try {
                        geocoder.getFromLocationName(name, 1)
                    } catch (e: Exception) {
                        null
                    }

                    if (!addresses.isNullOrEmpty()) {
                        val address = addresses[0]
                        val stopData = mapOf(
                            "name" to name,
                            "lat" to address.latitude,
                            "lng" to address.longitude
                        )

                        stopsRef.add(stopData)
                            .addOnSuccessListener { docRef ->
                                val finalStop = Stop(
                                    id = docRef.id,
                                    name = name,
                                    lat = address.latitude,
                                    lng = address.longitude
                                )
                                onResult(finalStop)
                            }
                            .addOnFailureListener {
                                Toast.makeText(requireContext(),
                                    "Error al guardar parada: $name", Toast.LENGTH_SHORT).show()
                                onResult(null)
                            }
                    } else {
                        Toast.makeText(requireContext(), "No se pudo geolocalizar: $name", Toast.LENGTH_SHORT).show()
                        onResult(null)
                    }
                }
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Error buscando parada: $name", Toast.LENGTH_SHORT).show()
                onResult(null)
            }
    }


    //devuelve la lista de objetos stop en el mismo orden
    private fun getOrCreateIntermediateStops(stopNames: List<String>,
                                             onComplete: (List<Stop>) -> Unit) {
        val resultStops = mutableListOf<Stop>()
        var processedCount = 0

        if (stopNames.isEmpty()) {
            onComplete(emptyList())
            return
        }

        stopNames.forEach { name ->
            getOrCreateStopByName(name) { stop ->
                if (stop != null) {
                    //se añade la parada resultado a la lista de paradas
                    resultStops.add(stop)
                } else {
                    Toast.makeText(requireContext(),
                        "Error procesando parada: $name", Toast.LENGTH_SHORT).show()
                }

                processedCount++

                if (processedCount == stopNames.size) {
                    // Ordenar resultado según el orden original de stopNames
                    val orderedStops = stopNames.mapNotNull { n -> resultStops.find { it.name == n } }
                    onComplete(orderedStops)
                }
            }
        }
    }

    //crea la ruta y las paradas por ruta
    private fun createRouteWithStops(
        origin: Stop,
        destination: Stop,
        intermediateStops: List<Stop>,
        onSuccess: (routeId: String) -> Unit,
        onError: () -> Unit) {
        //ubi en la base de datos
        val routeRef = db.collection("Routes").document()

        val route = Route(
            id = routeRef.id,
            idOrigin = origin.id ?: "",
            idDestination = destination.id ?: ""
        )

        // guardar la ruta principal
        routeRef.set(route)
            .addOnSuccessListener {
                val stopsInRouteRef = db.collection("StopsInRoute")

                // paradas totales en la ruta
                val allStopsOrdered = listOf(origin) + intermediateStops + listOf(destination)

                // guardar cada parada intermedia
                val tasks = allStopsOrdered.mapIndexed { index, stop ->
                    val stopInRoute = StopInRoute(
                        idRoute = route.id,
                        idStop = stop.id ?: "",
                        position = index
                    )
                    stopsInRouteRef.add(stopInRoute)
                }

                // Esperar a que todas las paradas se guarden
                Tasks.whenAllComplete(tasks)
                    .addOnSuccessListener {
                        onSuccess(route.id)
                    }
                    .addOnFailureListener {
                        Toast.makeText(requireContext(),
                            "Error al guardar paradas en la ruta", Toast.LENGTH_SHORT).show()
                        onError()
                    }
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Error al crear la ruta", Toast.LENGTH_SHORT).show()
                onError()
            }
    }



    //crea el viaje completo
    private fun createTrip(routeId: String) {
        val tripRef = db.collection("Trips").document()

        val price = binding.inputPrice.text.toString().toDoubleOrNull() ?: 0.0
        val places = binding.inputSeats.text.toString().toIntOrNull() ?: 1

        val trip = Trip(
            id = tripRef.id,
            idDriver = uid ?: return,
            idRoute = routeId,
            idCar = selectedCar?.id ?: "",
            description = binding.inputDescription.text.toString().trim(),
            price = price,
            places = places,
            date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(selectedDate.time),
            startTime = SimpleDateFormat("HH:mm", Locale.getDefault()).format(selectedTime.time),
            status = TripStatus.PENDING,
            createdAt = Date()
        )
        //guarda el viaje en la base de datos
        tripRef.set(trip)
            .addOnSuccessListener {
                // actualiza estadísticas del conductor
                uid?.let { updateDriverStatsTripCount(it) }

                showTripSuccessDialog()
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Error al publicar el viaje", Toast.LENGTH_SHORT).show()
            }
    }

    private fun updateDriverStatsTripCount(driverId: String) {
        val statsRef = db.collection("DriverStats").document(driverId)

        statsRef.get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    // Solo incrementa tripsPublished
                    statsRef.update("tripsPublished", com.google.firebase.firestore.FieldValue.increment(1))
                } else {
                    // Crear solo tripsPublished, no toques rating ni passengersTransported
                    val data = mapOf(
                        "idDriver" to driverId,
                        "tripsPublished" to 1
                    )
                    statsRef.set(data, com.google.firebase.firestore.SetOptions.merge())
                }
            }
    }




    //mensaje de exito al publicar un viaje
    private fun showTripSuccessDialog() {
        val activity = requireActivity()
        val dialogView = layoutInflater.inflate(R.layout.dialog_success_request, null)

        dialogView.findViewById<TextView>(R.id.tv_success).text = "¡Viaje publicado!"
        dialogView.findViewById<TextView>(R.id.tv_secondary).text = "Tu viaje ha sido creado exitosamente"

        val dialog = AlertDialog.Builder(activity)
            .setView(dialogView)
            .create()

        dialog.window?.setDimAmount(0.75f)
        dialog.window?.attributes?.windowAnimations = R.style.DialogFadeAnimation
        dialog.show()

        Handler(Looper.getMainLooper()).postDelayed({
            dialog.dismiss()
            activity.setResult(Activity.RESULT_OK)
            activity.finish()
        }, 1500)
    }
}