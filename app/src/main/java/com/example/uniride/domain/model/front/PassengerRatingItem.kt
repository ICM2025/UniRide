package com.example.uniride.domain.model.front

data class PassengerRatingItem(
    val idUser: String,
    val name: String,
    val profileUrl: String?,
    var stars: Int = 0
)
