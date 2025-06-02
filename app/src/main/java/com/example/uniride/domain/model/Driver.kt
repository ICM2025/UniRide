package com.example.uniride.domain.model

import java.util.Date

data class Driver(
    // mismo que el id del usuario
    val id: String = "",
    val imgLicense: String = "",
    val createdAt: Date = Date()
)
