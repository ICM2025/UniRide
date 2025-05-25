package com.example.uniride.domain.model

import java.util.Date

data class User(
    val id: String = "",
    val name: String = "",
    val email: String = "",
    val password: String = "",
    val imgProfile: String = "",
    val createdAt: Date = Date()
)
