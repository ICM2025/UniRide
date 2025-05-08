package com.example.uniride.domain.model

data class DriverTripItem(
    val travelOption: TravelOption,
    val acceptedCount: Int,
    val pendingCount: Int,
    val isFull: Boolean,
    val hasNewMessages: Boolean,
    val tripId: String = ""
)
