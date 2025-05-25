package com.example.uniride.domain.model

data class PassengerRequest(
    val id: String = "",
    // FK a Trip
    val idTrip: String = "",
    // FK a User
    val idUser: String = "",
    val status: RequestStatus = RequestStatus.PENDING
)
