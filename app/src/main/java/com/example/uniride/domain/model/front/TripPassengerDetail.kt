package com.example.uniride.domain.model.front

import com.example.uniride.domain.model.Stop
import com.example.uniride.domain.model.User

data class TripPassengerDetail(
    val tripInformation: TripInformation,
    val driverUser: User,
    val intermediateStops: List<Stop>,
    val tripId: String
)
