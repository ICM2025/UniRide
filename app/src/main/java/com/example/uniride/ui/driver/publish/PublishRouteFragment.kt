package com.example.uniride.ui.driver.publish

import android.app.Activity
import android.app.AlertDialog
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.uniride.R
import com.example.uniride.databinding.FragmentPublishRouteBinding
import com.example.uniride.domain.model.Vehicle
import com.google.android.gms.maps.model.LatLng
import android.content.Context
import android.content.SharedPreferences

class PublishRouteFragment : Fragment() {

    private var _binding: FragmentPublishRouteBinding? = null
    private val binding get() = _binding!!

    private lateinit var vehicleList: List<Vehicle>
    private val stopsList = mutableListOf<String>()
    private lateinit var sharedPreferences: SharedPreferences

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

        initStopFields()
        binding.btnAddStop.setOnClickListener {
            addNewStopField()
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}