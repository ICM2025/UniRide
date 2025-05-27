package com.example.uniride.ui.driver.publish

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
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
import com.example.uniride.domain.model.Vehicle
import java.util.Locale

class PublishRouteFragment : Fragment() {

    private var _binding: FragmentPublishRouteBinding? = null
    private val binding get() = _binding!!

    private lateinit var vehicleList: List<Vehicle>
    private val stopsList = mutableListOf<String>()
    private lateinit var sharedPreferences: SharedPreferences

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

        checkEditMode()
        setupVehicleSpinner()
        setupContinueButton()
        setupVoiceRecognition()

        initStopFields()
        binding.btnAddStop.setOnClickListener {
            addNewStopField()
        }

        if (isEditing && !editingTripId.isNullOrEmpty()) {
            loadTripDataForEditing()
            binding.btnContinue.text = "Guardar cambios"
        }
    }

    private fun checkEditMode() {
        val intent = requireActivity().intent
        isEditing = intent.getBooleanExtra("EDIT_MODE", false)

        if (isEditing) {
            editingTripId = sharedPreferences.getString("EDIT_TRIP_ID", null)
            loadTripDataForEditing()
            binding.btnContinue.text = "Actualizar viaje"
        }
    }

    private fun loadTripDataForEditing() {
        editingTripId?.let { tripId ->
            // Cargar datos del viaje a editar
            val origin = sharedPreferences.getString("EDIT_ORIGIN", "")
            val destination = sharedPreferences.getString("EDIT_DESTINATION", "")
            val stopsCount = sharedPreferences.getInt("EDIT_STOPS_COUNT", 0)

            binding.inputOrigin.setText(origin)
            binding.inputDestination.setText(destination)

            // Limpiar stops existentes
            stopsList.clear()
            binding.stopsContainer.removeAllViews()

            // Cargar paradas existentes
            for (i in 0 until stopsCount) {
                val stop = sharedPreferences.getString("EDIT_STOP_$i", "")
                if (!stop.isNullOrEmpty()) {
                    addNewStopField()
                    val lastStopView = binding.stopsContainer.getChildAt(binding.stopsContainer.childCount - 1)
                    val stopEditText = lastStopView.findViewById<EditText>(R.id.et_stop)
                    stopEditText.setText(stop)
                }
            }

            // Si no hay paradas, agregar un campo vacío
            if (stopsCount == 0) {
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
    private fun setupVehicleSpinner() {
        vehicleList = loadVehiclesForUser() // Simulación o llamada real a DB

        val vehicleNames = if (vehicleList.isEmpty()) {
            listOf("Agregar vehículo")
        } else {
            vehicleList.map { "${it.brand} ${it.model} (${it.licensePlate})" } + "Agregar vehículo"
        }

        val adapter =
            ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, vehicleNames)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerCar.adapter = adapter

        binding.spinnerCar.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>,
                view: View?,
                position: Int,
                id: Long
            ) {
                val selected = parent.getItemAtPosition(position).toString()
                if (selected == "Agregar vehículo") {
                    findNavController().navigate(R.id.action_publishRouteFragment_to_addVehicleFragment)
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
    }

    private fun setupContinueButton() {
        binding.btnContinue.setOnClickListener {
            val originAddress = binding.inputOrigin.text.toString()
            val destinationAddress = binding.inputDestination.text.toString()

            if (originAddress.isEmpty() || destinationAddress.isEmpty()) {
                Toast.makeText(requireContext(), "Por favor completa origen y destino", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

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
                updateExistingTrip(originAddress, destinationAddress, stopsList)
            } else {
                saveRouteData(originAddress, destinationAddress, stopsList)
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

    private fun saveRouteData(origin: String, destination: String, stops: List<String>) {
        val editor = sharedPreferences.edit()

        val tripId = if (isEditing && editingTripId != null) {
            editingTripId!!
        } else {
            System.currentTimeMillis().toString()
        }

        editor.putString("TRIP_${tripId}_ORIGIN", origin)
        editor.putString("TRIP_${tripId}_DESTINATION", destination)
        editor.putInt("TRIP_${tripId}_STOPS_COUNT", stops.size)

        stops.forEachIndexed { index, stop ->
            editor.putString("TRIP_${tripId}_STOP_$index", stop)
        }

        if (!isEditing) {
            val tripIds = sharedPreferences.getStringSet("SAVED_TRIP_IDS", mutableSetOf()) ?: mutableSetOf()
            tripIds.add(tripId)
            editor.putStringSet("SAVED_TRIP_IDS", tripIds)
            editor.putBoolean("HAS_PUBLISHED_ROUTE", true)
        }

        editor.putBoolean("HAS_ACTIVE_ROUTE", false)

        // Limpiar datos temporales de edición
        if (isEditing) {
            editor.remove("EDIT_TRIP_ID")
            editor.remove("EDIT_ORIGIN")
            editor.remove("EDIT_DESTINATION")
            editor.remove("EDIT_STOPS_COUNT")
            val editStopsCount = sharedPreferences.getInt("EDIT_STOPS_COUNT", 0)
            for (i in 0 until editStopsCount) {
                editor.remove("EDIT_STOP_$i")
            }
        }

        editor.apply()
    }

    private fun updateExistingTrip(origin: String, destination: String, stops: List<String>) {
        editingTripId?.let { tripId ->
            val editor = sharedPreferences.edit()

            // Eliminar las paradas anteriores
            val oldStopsCount = sharedPreferences.getInt("TRIP_${tripId}_STOPS_COUNT", 0)
            for (i in 0 until oldStopsCount) {
                editor.remove("TRIP_${tripId}_STOP_$i")
            }

            // Actualizar los datos
            editor.putString("TRIP_${tripId}_ORIGIN", origin)
            editor.putString("TRIP_${tripId}_DESTINATION", destination)
            editor.putInt("TRIP_${tripId}_STOPS_COUNT", stops.size)

            stops.forEachIndexed { index, stop ->
                editor.putString("TRIP_${tripId}_STOP_$index", stop)
            }

            // Si este viaje es el activo, actualizar también las rutas activas
            val activeOrigin = sharedPreferences.getString("ROUTE_ORIGIN", "")
            val oldOrigin = sharedPreferences.getString("TRIP_${tripId}_ORIGIN", "")

            if (activeOrigin == oldOrigin) {
                editor.putString("ROUTE_ORIGIN", origin)
                editor.putString("ROUTE_DESTINATION", destination)
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

            editor.apply()
        }
    }

    //simulando que tiene carros registrados
    private fun loadVehiclesForUser(): List<Vehicle> {
        // Simulado
        return listOf(
            Vehicle("Toyota", "Corolla", 2020, "Blanco", "ABC123", listOf()),
            Vehicle("Mazda", "3", 2022, "Gris", "XYZ987", listOf())
        )
    }

    private fun initStopFields() {
        addNewStopField() // primer campo por defecto
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