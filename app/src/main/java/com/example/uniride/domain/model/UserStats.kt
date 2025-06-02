package com.example.uniride.domain.model

data class UserStats(
    // FK a User
    val idUser: String = "",
    val tripsTaken: Int = 0,
    val rating: Double = 0.0
)
