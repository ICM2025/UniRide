package com.example.uniride.ui.driver.vehicles

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.navigation.fragment.findNavController
import com.example.uniride.R
import com.example.uniride.databinding.FragmentAddVehicleBinding
import com.example.uniride.domain.model.Vehicle
import com.example.uniride.domain.model.VehicleBrand

class AddVehicleFragment : Fragment() {

    private var _binding: FragmentAddVehicleBinding? = null
    private val binding get() = _binding!!

    private lateinit var selectedBrand: VehicleBrand

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddVehicleBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupBrandSpinner()
        setupSaveButton()
        binding.btnBack.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }
    }

    private fun setupBrandSpinner() {
        val brandNames = VehicleBrand.allBrands().map { it.displayName }
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, brandNames)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerBrand.adapter = adapter

        binding.spinnerBrand.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                selectedBrand = VehicleBrand.fromDisplayName(parent.getItemAtPosition(position).toString())
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                selectedBrand = VehicleBrand.Otro
            }
        }
    }

    private fun setupSaveButton() {
        binding.btnSave.setOnClickListener {
            val model = binding.inputModel.text.toString().trim()
            val year = binding.inputYear.text.toString().toIntOrNull()
            val color = binding.inputColor.text.toString().trim()
            val plate = binding.inputPlate.text.toString().trim()

            if (model.isEmpty() || year == null || color.isEmpty() || plate.isEmpty()) {
                Toast.makeText(requireContext(), "Completa todos los campos", Toast.LENGTH_SHORT).show()
                //esto es para salir del on click listener
                return@setOnClickListener
            }

            val newVehicle = Vehicle(
                brand = selectedBrand.displayName,
                model = model,
                year = year,
                color = color,
                licensePlate = plate,
                imageUrls = emptyList() // Se puede modificar luego
            )

            // acá se debería guardar en la DB
            findNavController().navigate(R.id.action_addVehicleFragment_to_publishRouteFragment)
        }
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
