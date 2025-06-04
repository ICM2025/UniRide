package com.example.uniride.domain.model

data class DriverStats(
    // FK a Driver
    val idDriver: String = "",
    val tripsPublished: Int = 0,
    val rating: Double = 0.0,
    val passengersTransported: Int = 0
)
