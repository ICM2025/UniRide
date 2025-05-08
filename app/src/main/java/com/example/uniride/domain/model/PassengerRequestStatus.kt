package com.example.uniride.domain.model

import com.example.uniride.R

sealed class PassengerRequestStatus(
    val label: String,
    val icon: Int,
    val color: Int
) {
    object Pending : PassengerRequestStatus("Pendiente", R.drawable.ic_pending, R.color.status_pending)
    object Accepted : PassengerRequestStatus("Aceptado", R.drawable.ic_check_circle, R.color.status_accepted)
    object Rejected : PassengerRequestStatus("Rechazado", R.drawable.ic_rejected, R.color.status_rejected)
    object Finished : PassengerRequestStatus("Terminado", R.drawable.ic_check, R.color.status_finished)
}
