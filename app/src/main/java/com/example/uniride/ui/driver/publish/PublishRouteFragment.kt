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

        // Initialize SharedPreferences
        sharedPreferences = requireActivity().getSharedPreferences("route_data", Context.MODE_PRIVATE)

        setupVehicleSpinner()
        setupContinueButton()
        setupVoiceRecognition()

        initStopFields()
        binding.btnAddStop.setOnClickListener {
            addNewStopField()
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

        // Listeners para detectar en qué campo está el foco
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
            // Get the origin and destination locations
            val originAddress = binding.inputOrigin.text.toString()
            val destinationAddress = binding.inputDestination.text.toString()

            // Clear previous stops
            stopsList.clear()

            // Get all intermediate stops
            for (i in 0 until binding.stopsContainer.childCount) {
                val stopView = binding.stopsContainer.getChildAt(i)
                val stopEditText = stopView.findViewById<EditText>(R.id.et_stop)
                val stopText = stopEditText.text.toString().trim()
                if (stopText.isNotEmpty()) {
                    stopsList.add(stopText)
                }
            }

            // Save route data to SharedPreferences
            saveRouteData(originAddress, destinationAddress, stopsList)

            val activity = requireActivity()

            val dialogView = layoutInflater.inflate(R.layout.dialog_success_request, null)

            dialogView.findViewById<TextView>(R.id.tv_success).text = "¡Viaje publicado!"
            dialogView.findViewById<TextView>(R.id.tv_secondary).text =
                "Tu viaje ha sido creado exitosamente"

            val dialog = AlertDialog.Builder(activity)
                .setView(dialogView)
                .create()

            dialog.window?.setDimAmount(0.75f)
            dialog.window?.attributes?.windowAnimations = R.style.DialogFadeAnimation
            dialog.show()

            Handler(Looper.getMainLooper()).postDelayed({
                dialog.dismiss()

                // Set result to notify the Driver Home that a route was published
                activity.setResult(Activity.RESULT_OK)
                activity.finish()
            }, 1500)
        }
    }

    private fun saveRouteData(origin: String, destination: String, stops: List<String>) {
        val editor = sharedPreferences.edit()
        editor.putString("ROUTE_ORIGIN", origin)
        editor.putString("ROUTE_DESTINATION", destination)
        editor.putInt("ROUTE_STOPS_COUNT", stops.size)

        // Save each stop
        stops.forEachIndexed { index, stop ->
            editor.putString("ROUTE_STOP_$index", stop)
        }

        // Set flag to indicate a route has been published
        editor.putBoolean("HAS_PUBLISHED_ROUTE", true)
        editor.putBoolean("HAS_ACTIVE_ROUTE", false)

        editor.apply()
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

        // acá luego se añade para el de cambiar de orden

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
                    Toast.makeText(
                        requireContext(),
                        "Permiso de micrófono denegado",
                        Toast.LENGTH_SHORT
                    ).show()
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