package com.example.uniride.domain.model

data class PassengerRequest(
    val passengerName: String,
    val profileDrawableRes: Int,
    val destination: String,
    val status: PassengerRequestStatus
)
