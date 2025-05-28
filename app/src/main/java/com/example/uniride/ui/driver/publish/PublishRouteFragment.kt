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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Calendar
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
        // Verificar si venimos del intent de la activity
        val intent = requireActivity().intent
        val isEditingFromIntent = intent.getBooleanExtra("IS_EDITING", false)

        // También verificar SharedPreferences
        val isEditingFromPrefs = sharedPreferences.getBoolean("IS_EDITING_TRIP", false)

        isEditing = isEditingFromIntent || isEditingFromPrefs

        if (isEditing) {
            // Obtener el tripId del intent o SharedPreferences
            editingTripId = intent.getStringExtra("TRIP_ID")
                ?: sharedPreferences.getString("EDITING_TRIP_ID", null)

            if (!editingTripId.isNullOrEmpty()) {
                loadTripDataForEditing()
                binding.btnContinue.text = "Actualizar viaje"
            }
        }
    }

    private fun loadTripDataForEditing() {
        editingTripId?.let { tripId ->
            // Cargar datos del viaje a editar usando las claves correctas
            val origin = sharedPreferences.getString("EDIT_ROUTE_ORIGIN", "")
                ?: sharedPreferences.getString("TRIP_${tripId}_ORIGIN", "")

            val destination = sharedPreferences.getString("EDIT_ROUTE_DESTINATION", "")
                ?: sharedPreferences.getString("TRIP_${tripId}_DESTINATION", "")

            val stopsCount = sharedPreferences.getInt("EDIT_ROUTE_STOPS_COUNT", 0).takeIf { it > 0 }
                ?: sharedPreferences.getInt("TRIP_${tripId}_STOPS_COUNT", 0)

            // Cargar fecha y hora guardadas
            val savedDate = sharedPreferences.getString("TRIP_${tripId}_DATE", "")
            val savedTime = sharedPreferences.getString("TRIP_${tripId}_TIME", "")

            // Establecer los valores en los campos de texto
            binding.inputOrigin.setText(origin)
            binding.inputDestination.setText(destination)

            // Establecer fecha y hora si existen
            if (!savedDate.isNullOrEmpty()) {
                binding.inputDate.setText(savedDate)
                try {
                    selectedDate.time = dateFormat.parse(savedDate) ?: Calendar.getInstance().time
                } catch (e: Exception) {
                    selectedDate = Calendar.getInstance()
                }
            }

            if (!savedTime.isNullOrEmpty()) {
                binding.inputTime.setText(savedTime)
                try {
                    val timeCalendar = Calendar.getInstance()
                    timeCalendar.time = timeFormat.parse(savedTime) ?: Calendar.getInstance().time
                    selectedTime.set(Calendar.HOUR_OF_DAY, timeCalendar.get(Calendar.HOUR_OF_DAY))
                    selectedTime.set(Calendar.MINUTE, timeCalendar.get(Calendar.MINUTE))
                } catch (e: Exception) {
                    selectedTime = Calendar.getInstance()
                }
            }

            // Limpiar stops existentes
            stopsList.clear()
            binding.stopsContainer.removeAllViews()

            // Cargar paradas existentes
            for (i in 0 until stopsCount) {
                val stop = sharedPreferences.getString("EDIT_ROUTE_STOP_$i", "")
                    ?: sharedPreferences.getString("TRIP_${tripId}_STOP_$i", "")

                if (!stop.isNullOrEmpty()) {
                    addNewStopField()
                    val lastStopView = binding.stopsContainer.getChildAt(binding.stopsContainer.childCount - 1)
                    val stopEditText = lastStopView.findViewById<EditText>(R.id.et_stop)
                    stopEditText.setText(stop)
                }
            }

            // Si no hay paradas, agregar un campo vacío por defecto
            if (binding.stopsContainer.childCount == 0) {
                addNewStopField()
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
            val originAddress = binding.inputOrigin.text.toString().trim()
            val destinationAddress = binding.inputDestination.text.toString().trim()
            val selectedDateStr = binding.inputDate.text.toString().trim()
            val selectedTimeStr = binding.inputTime.text.toString().trim()

            if (originAddress.isEmpty() || destinationAddress.isEmpty()) {
                Toast.makeText(requireContext(), "Por favor completa origen y destino", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (selectedDateStr.isEmpty() || selectedTimeStr.isEmpty()) {
                Toast.makeText(requireContext(), "Por favor selecciona fecha y hora", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Validar que la fecha y hora no sean en el pasado
            val selectedDateTime = Calendar.getInstance()
            selectedDateTime.set(Calendar.YEAR, selectedDate.get(Calendar.YEAR))
            selectedDateTime.set(Calendar.MONTH, selectedDate.get(Calendar.MONTH))
            selectedDateTime.set(Calendar.DAY_OF_MONTH, selectedDate.get(Calendar.DAY_OF_MONTH))
            selectedDateTime.set(Calendar.HOUR_OF_DAY, selectedTime.get(Calendar.HOUR_OF_DAY))
            selectedDateTime.set(Calendar.MINUTE, selectedTime.get(Calendar.MINUTE))

            if (selectedDateTime.timeInMillis < System.currentTimeMillis()) {
                Toast.makeText(requireContext(), "No puedes programar un viaje en el pasado", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Recopilar paradas
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
                updateExistingTrip(originAddress, destinationAddress, stopsList, selectedDateStr, selectedTimeStr)
            } else {
                saveRouteData(originAddress, destinationAddress, stopsList, selectedDateStr, selectedTimeStr)
            }

            val activity = requireActivity()
            val dialogView = layoutInflater.inflate(R.layout.dialog_success_request, null)

            val successText = if (isEditing) "¡Viaje actualizado!" else "¡Viaje publicado!"
            val secondaryText = if (isEditing) "Tu viaje ha sido actualizado exitosamente" else "Tu viaje ha sido creado exitosamente"

            dialogView.findViewById<TextView>(R.id.tv_success).text = successText
            dialogView.findViewById<TextView>(R.id.tv_secondary).text = secondaryText

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

    private fun saveRouteData(origin: String, destination: String, stops: List<String>, date: String, time: String) {
        val editor = sharedPreferences.edit()

        // Generar nuevo ID para viaje nuevo
        val tripId = System.currentTimeMillis().toString()

        editor.putString("TRIP_${tripId}_ORIGIN", origin)
        editor.putString("TRIP_${tripId}_DESTINATION", destination)
        editor.putString("TRIP_${tripId}_DATE", date)
        editor.putString("TRIP_${tripId}_TIME", time)
        editor.putInt("TRIP_${tripId}_STOPS_COUNT", stops.size)

        stops.forEachIndexed { index, stop ->
            editor.putString("TRIP_${tripId}_STOP_$index", stop)
        }

        // Agregar a la lista de viajes guardados
        val tripIds = sharedPreferences.getStringSet("SAVED_TRIP_IDS", mutableSetOf())?.toMutableSet() ?: mutableSetOf()
        tripIds.add(tripId)
        editor.putStringSet("SAVED_TRIP_IDS", tripIds)
        editor.putBoolean("HAS_PUBLISHED_ROUTE", true)

        editor.apply()
    }

    private fun updateExistingTrip(origin: String, destination: String, stops: List<String>, date: String, time: String) {
        editingTripId?.let { tripId ->
            val editor = sharedPreferences.edit()

            // Limpiar paradas anteriores
            val oldStopsCount = sharedPreferences.getInt("TRIP_${tripId}_STOPS_COUNT", 0)
            for (i in 0 until oldStopsCount) {
                editor.remove("TRIP_${tripId}_STOP_$i")
            }

            // Actualizar los datos del viaje existente
            editor.putString("TRIP_${tripId}_ORIGIN", origin)
            editor.putString("TRIP_${tripId}_DESTINATION", destination)
            editor.putString("TRIP_${tripId}_DATE", date)
            editor.putString("TRIP_${tripId}_TIME", time)
            editor.putInt("TRIP_${tripId}_STOPS_COUNT", stops.size)

            stops.forEachIndexed { index, stop ->
                editor.putString("TRIP_${tripId}_STOP_$index", stop)
            }

            // Si este viaje es el activo, actualizar también las rutas activas
            val activeTripId = sharedPreferences.getString("ACTIVE_TRIP_ID", "")
            if (activeTripId == tripId) {
                editor.putString("ROUTE_ORIGIN", origin)
                editor.putString("ROUTE_DESTINATION", destination)
                editor.putString("ROUTE_DATE", date)
                editor.putString("ROUTE_TIME", time)
                editor.putInt("ROUTE_STOPS_COUNT", stops.size)

                // Limpiar paradas activas anteriores
                val oldActiveStopsCount = sharedPreferences.getInt("ROUTE_STOPS_COUNT", 0)
                for (i in 0 until oldActiveStopsCount) {
                    editor.remove("ROUTE_STOP_$i")
                }

                // Agregar nuevas paradas activas
                stops.forEachIndexed { index, stop ->
                    editor.putString("ROUTE_STOP_$index", stop)
                }
            }

            // Limpiar datos temporales de edición
            editor.remove("IS_EDITING_TRIP")
            editor.remove("EDITING_TRIP_ID")
            editor.remove("EDIT_ROUTE_ORIGIN")
            editor.remove("EDIT_ROUTE_DESTINATION")
            editor.remove("EDIT_ROUTE_STOPS_COUNT")

            val editStopsCount = sharedPreferences.getInt("EDIT_ROUTE_STOPS_COUNT", 0)
            for (i in 0 until editStopsCount) {
                editor.remove("EDIT_ROUTE_STOP_$i")
            }

            editor.apply()
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
}