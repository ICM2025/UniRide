package com.example.uniride.domain.model

import java.time.LocalDateTime

data class TravelRequest(
    val travelOption: TravelOption,
    val requestDate: LocalDateTime,
    val status: TravelRequestStatus
)
