package com.example.uniride.ui

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class MainViewModel : ViewModel() {

    // Estado actual del modo: true = pasajero, false = conductor
    //mutable state flow para observar cambios usando collect, collectLatest en corrutina
    private val _isPassengerMode = MutableStateFlow(true)
    //state flow para que el valor no se pueda modificar fuera del view model
    val isPassengerMode: StateFlow<Boolean> = _isPassengerMode.asStateFlow()

    fun switchToPassenger() {
        _isPassengerMode.value = true
    }

    fun switchToDriver() {
        _isPassengerMode.value = false
    }
}
