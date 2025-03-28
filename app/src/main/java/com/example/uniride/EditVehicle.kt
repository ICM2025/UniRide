package com.example.uniride

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.uniride.databinding.ActivityEditVehicleBinding

class EditVehicle : AppCompatActivity() {
    private lateinit var binding: ActivityEditVehicleBinding

    // Indica si estamos editando un vehículo existente
    private var isEditMode = false
    private var existingVehicleId: String? = null

    companion object {
        const val RESULT_VEHICLE_SAVED = 1001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditVehicleBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Recuperar datos desde el Intent
        val extras = intent.extras
        if (extras != null) {
            isEditMode = true
            existingVehicleId = extras.getString("id")
            populateVehicleDetails(
                make = extras.getString("make", ""),
                model = extras.getString("model", ""),
                year = extras.getInt("year", 0),
                licensePlate = extras.getString("licensePlate", ""),
                capacity = extras.getInt("capacity", 1),
                color = extras.getString("color", "")
            )
        }

        setupButtonListeners()
    }

    private fun setupButtonListeners() {
        binding.btnBack.setOnClickListener { finish() }
        binding.btnCancel.setOnClickListener { finish() }
        binding.btnSaveVehicle.setOnClickListener { saveVehicle() }
    }

    private fun populateVehicleDetails(make: String, model: String, year: Int, licensePlate: String, capacity: Int, color: String?) {
        binding.apply {
            etMake.setText(make)
            etModel.setText(model)
            etYear.setText(year.toString())
            etLicensePlate.setText(licensePlate)
            etCapacity.setText(capacity.toString())
            etColor.setText(color ?: "")
        }
    }

    private fun saveVehicle() {
        if (!validateInputs()) return

        val resultIntent = Intent().apply {
            putExtra("id", existingVehicleId)
            putExtra("make", binding.etMake.text.toString().trim())
            putExtra("model", binding.etModel.text.toString().trim())
            putExtra("year", binding.etYear.text.toString().toInt())
            putExtra("licensePlate", binding.etLicensePlate.text.toString().trim())
            putExtra("capacity", binding.etCapacity.text.toString().toInt())
            putExtra("color", binding.etColor.text.toString().takeIf { it.isNotBlank() })
        }

        setResult(RESULT_VEHICLE_SAVED, resultIntent)
        finish()
    }

    private fun validateInputs(): Boolean {
        var isValid = true

        if (binding.etMake.text.isNullOrBlank()) {
            binding.etMake.error = "Marca es requerida"
            isValid = false
        }
        if (binding.etModel.text.isNullOrBlank()) {
            binding.etModel.error = "Modelo es requerido"
            isValid = false
        }

        val yearText = binding.etYear.text.toString()
        if (yearText.isBlank()) {
            binding.etYear.error = "Año es requerido"
            isValid = false
        } else {
            val year = yearText.toIntOrNull()
            if (year == null || year < 1900 || year > android.icu.util.Calendar.getInstance().get(android.icu.util.Calendar.YEAR) + 1) {
                binding.etYear.error = "Año inválido"
                isValid = false
            }
        }

        if (binding.etLicensePlate.text.isNullOrBlank()) {
            binding.etLicensePlate.error = "Placa es requerida"
            isValid = false
        }

        val capacityText = binding.etCapacity.text.toString()
        if (capacityText.isBlank()) {
            binding.etCapacity.error = "Capacidad es requerida"
            isValid = false
        } else {
            val capacity = capacityText.toIntOrNull()
            if (capacity == null || capacity <= 0 || capacity > 10) {
                binding.etCapacity.error = "Capacidad inválida (1-10)"
                isValid = false
            }
        }

        return isValid
    }
}
