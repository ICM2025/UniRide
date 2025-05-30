package com.example.uniride.domain.model

data class PassengerRequestOLD(
    val passengerName: String,
    val destination: String,
    val status: PassengerRequestStatus,
    val profileImg: Int,
    val university: String,
    val email: String,
    val tripCount: Int,
    val rating: Double,
    val reviewsCount: Int,
    val requestId: String = ""
)
