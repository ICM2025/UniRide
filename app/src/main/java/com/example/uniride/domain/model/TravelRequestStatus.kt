package com.example.uniride.domain.model

import com.example.uniride.R

sealed class TravelRequestStatus(
    val label: String,
    val colorRes: Int,
    val iconRes: Int
) {
    object Pending : TravelRequestStatus("Pendiente", R.color.status_pending, R.drawable.ic_pending)
    object Accepted : TravelRequestStatus("Aceptado", R.color.status_accepted, R.drawable.ic_check_circle)
    object Rejected : TravelRequestStatus("Rechazado", R.color.status_rejected, R.drawable.ic_rejected)
    object Finished : TravelRequestStatus("Terminado", R.color.status_finished, R.drawable.ic_done)
}
