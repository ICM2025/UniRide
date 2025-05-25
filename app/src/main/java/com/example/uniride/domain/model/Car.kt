package com.example.uniride.domain.model

data class Car(
    val id: String = "",
    // FK a Driver
    val idDriver: String = "",
    val brand: String = "",
    val model: String = "",
    val year: Int = 0,
    val color: String = "",
    val numberPlate: String = "",
    val images: List<String> = emptyList()
)
