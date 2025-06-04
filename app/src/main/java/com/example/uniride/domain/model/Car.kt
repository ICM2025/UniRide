package com.example.uniride.domain.model

data class Car(
    var id: String = "",
    // FK a Driver
    val idDriver: String = "",
    val brand: String = "",
    val model: String = "",
    val year: Int = 0,
    val color: String = "",
    val licensePlate: String = "",
    var images: List<String> = emptyList()
)
