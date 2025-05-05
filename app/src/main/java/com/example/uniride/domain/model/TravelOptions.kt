package com.example.uniride.domain.model

data class TravelOption(
    //val id: String,
    val driverName: String,
    val description: String,
    val price: Int,
    val driverImage: Int,
    val drawableResId: Int,
    val availableSeats: Int,
    val origin: String,
    val destination: String,
    val departureTime: String,  // por ejemplo: "17:45"
    val intermediateStops: List<String>
)
