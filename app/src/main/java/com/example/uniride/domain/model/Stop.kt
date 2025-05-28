package com.example.uniride.domain.model

data class Stop(
    //se asigna en firebase
    val id: String? = null,
    val name: String = "",
    val lat: Double = 0.0,
    val lng: Double = 0.0
)