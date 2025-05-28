package com.example.uniride.domain.model.front

import java.time.LocalDate

data class TripInformation (
    val carIcon: Int,
    val availableSeats: Int,
    val origin: String,
    val destination: String,
    val departureTime: String,
    val travelDate: LocalDate
)