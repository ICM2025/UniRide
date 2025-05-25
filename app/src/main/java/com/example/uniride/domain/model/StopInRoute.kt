package com.example.uniride.domain.model

data class StopInRoute(
    // FK a Route
    val idRoute: String = "",
    // FK a Stop
    val idStop: String = "",
    val position: Int = 0,
    val estimatedTime: String? = null
)
