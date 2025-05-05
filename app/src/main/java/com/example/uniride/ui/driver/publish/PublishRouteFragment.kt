package com.example.uniride.ui.driver.publish

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.uniride.R
import com.example.uniride.databinding.FragmentPublishRouteBinding
import com.example.uniride.domain.model.Vehicle

class PublishRouteFragment : Fragment() {

    private var _binding: FragmentPublishRouteBinding? = null
    private val binding get() = _binding!!

    private lateinit var vehicleList: List<Vehicle>

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPublishRouteBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
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

        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, vehicleNames)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerCar.adapter = adapter

        binding.spinnerCar.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
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
            // Validaciones y navegación siguiente
            Toast.makeText(requireContext(), "Viaje validado", Toast.LENGTH_SHORT).show()
        }
    }

    //simulando que tiene carros registrados
    private fun loadVehiclesForUser(): List<Vehicle> {
        // Simulado
        return listOf(
            Vehicle("Toyota", "Corolla", 2020, "Blanco", "ABC123", listOf()),
            Vehicle( "Mazda", "3", 2022, "Gris", "XYZ987", listOf())
        )
    }

    private fun initStopFields() {
        addNewStopField() // primer campo por defecto
    }


    private fun addNewStopField() {
        val stopView = layoutInflater.inflate(R.layout.item_stop_field, binding.stopsContainer, false)

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
