package com.example.uniride.domain.model

import java.util.Date

data class User(
    val id: String = "",
    val username: String = "",
    val email: String = "",
    //val password: String = "", (la contra la maneja firebase)
    val imgProfile: String = "",
    val createdAt: Date = Date()
)
