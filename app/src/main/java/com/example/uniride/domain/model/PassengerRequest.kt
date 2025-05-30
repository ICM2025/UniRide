package com.example.uniride.domain.model

data class PassengerRequest(
    val id: String = "",
    // FK a Trip
    val idTrip: String = "",
    // FK a User
    val idUser: String = "",
    val status: RequestStatus = RequestStatus.PENDING,
    val attemptCount: Int = 1, // NUEVO: Contador de intentos
    val createdAt: Long = System.currentTimeMillis()
)
