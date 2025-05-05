package com.example.uniride.domain.model

import java.util.UUID

data class Vehicle(
//    val id: String = UUID.randomUUID().toString(),
    val brand: String,
    val model: String,
    val year: Int,
    val color: String,
    val licensePlate: String,
    val imageUrls: List<String> = emptyList()
)
