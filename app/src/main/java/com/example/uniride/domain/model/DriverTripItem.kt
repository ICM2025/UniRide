package com.example.uniride.domain.model

import com.example.uniride.domain.model.front.TripInformation

data class DriverTripItem(
    val tripInformation: TripInformation,
    val acceptedCount: Int,
    val pendingCount: Int,
    val isFull: Boolean,
    val hasNewMessages: Boolean,
    val tripId: String = ""
)
