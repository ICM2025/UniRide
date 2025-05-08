package com.example.uniride.domain.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.uniride.databinding.ItemDriverTripBinding
import com.example.uniride.domain.model.DriverTripItem

class DriverTripsAdapter(
    private val items: List<DriverTripItem>,
    private val onClick: (DriverTripItem) -> Unit
) : RecyclerView.Adapter<DriverTripsAdapter.TripViewHolder>() {

    inner class TripViewHolder(val binding: ItemDriverTripBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TripViewHolder {
        val binding = ItemDriverTripBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return TripViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TripViewHolder, position: Int) {
        val item = items[position]
        val travel = item.travelOption

        holder.binding.apply {
            // Ruta
            tvRoute.text = "${travel.origin} → ${travel.destination}"

            // Fecha y hora
            tvDateTime.text = "${travel.travelDate} - ${travel.departureTime}"

            // Cupos
            tvPassengers.text = "${item.acceptedCount}/${travel.availableSeats} pasajeros"
            progressCircle.max = travel.availableSeats
            progressCircle.progress = item.acceptedCount

            // Solicitudes pendientes
            tvPendingRequests.text = "${item.pendingCount} pendientes"
            tvPendingRequests.visibility = if (item.pendingCount > 0) View.VISIBLE else View.GONE

            // Icono de vehículo
            ivVehicleIcon.setImageResource(travel.drawableResId)

            // si está lleno hacemos un enfoque opaco
            root.alpha = if (item.isFull) 0.6f else 1.0f

            // listener a cada instancia del item usando root
            root.setOnClickListener {
                onClick(item)
            }

        }
    }

    override fun getItemCount(): Int = items.size
}
