package com.example.uniride.domain.model

data class Route(
    val id: String = "",
    // FK a Stop
    val idOrigin: String = "",
    // FK a Stop
    val idDestination: String = ""
)
