package com.example.uniride.domain.model

import java.util.Date

data class Trip(
    val id: String = "",
    // FK a Driver
    val idDriver: String = "",
    // FK a Route
    val idRoute: String = "",
    val description: String = "",
    val price: Double = 0.0,
    val places: Int = 0,
    val date: String = "",       // "YYYY-MM-DD"
    val startTime: String = "",  // "HH:mm"
    val status: TripStatus = TripStatus.PENDING,
    val createdAt: Date = Date()
)
