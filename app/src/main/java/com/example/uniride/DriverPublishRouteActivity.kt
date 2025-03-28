package com.example.uniride

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.uniride.databinding.ActivityDriverPublishRouteBinding
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class DriverPublishRouteActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDriverPublishRouteBinding
    private var selectedCalendar = Calendar.getInstance()
    private var seatsCount = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDriverPublishRouteBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupListeners()
    }

    private fun setupListeners() {
        binding.btnBack.setOnClickListener {
            finish()
        }

        binding.btnDecreaseSeats.setOnClickListener {
            if (seatsCount > 1) {
                seatsCount--
                updateSeatsCount()
            }
        }

        binding.btnIncreaseSeats.setOnClickListener {
            if (seatsCount < 6) {
                seatsCount++
                updateSeatsCount()
            }
        }

        binding.btnSelectDate.setOnClickListener {
            showDatePicker()
        }

        binding.btnSelectTime.setOnClickListener {
            showTimePicker()
        }

        binding.btnPublishRoute.setOnClickListener {
            publishRoute()
        }
    }

    private fun updateSeatsCount() {
        binding.tvSeatsCount.text = seatsCount.toString()
    }

    private fun showDatePicker() {
        val year = selectedCalendar.get(Calendar.YEAR)
        val month = selectedCalendar.get(Calendar.MONTH)
        val day = selectedCalendar.get(Calendar.DAY_OF_MONTH)

        val datePickerDialog = DatePickerDialog(
            this,
            { _, selectedYear, selectedMonth, selectedDay ->
                selectedCalendar.set(Calendar.YEAR, selectedYear)
                selectedCalendar.set(Calendar.MONTH, selectedMonth)
                selectedCalendar.set(Calendar.DAY_OF_MONTH, selectedDay)
                updateSelectedDateTime()
            },
            year,
            month,
            day
        )
        datePickerDialog.show()
    }

    private fun showTimePicker() {
        val hour = selectedCalendar.get(Calendar.HOUR_OF_DAY)
        val minute = selectedCalendar.get(Calendar.MINUTE)

        val timePickerDialog = TimePickerDialog(
            this,
            { _, selectedHour, selectedMinute ->
                selectedCalendar.set(Calendar.HOUR_OF_DAY, selectedHour)
                selectedCalendar.set(Calendar.MINUTE, selectedMinute)
                updateSelectedDateTime()
            },
            hour,
            minute,
            true
        )
        timePickerDialog.show()
    }

    private fun updateSelectedDateTime() {
        val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        val formattedDateTime = dateFormat.format(selectedCalendar.time)
        binding.tvSelectedDateTime.text = formattedDateTime
    }

    private fun publishRoute() {
        when {
            binding.etOrigin.text.toString().isBlank() -> {
                Toast.makeText(this, "Ingrese el punto de origen", Toast.LENGTH_SHORT).show()
                return
            }
            binding.etDestination.text.toString().isBlank() -> {
                Toast.makeText(this, "Ingrese el punto de destino", Toast.LENGTH_SHORT).show()
                return
            }
            binding.tvSelectedDateTime.text == "No seleccionado" -> {
                Toast.makeText(this, "Seleccione la fecha y hora de la ruta", Toast.LENGTH_SHORT).show()
                return
            }
        }
        Toast.makeText(this, "Ruta publicada con éxito", Toast.LENGTH_LONG).show()
        finish()
    }
}