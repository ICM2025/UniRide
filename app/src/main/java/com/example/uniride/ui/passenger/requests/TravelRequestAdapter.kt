package com.example.uniride.ui.passenger.requests

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.uniride.R
import com.example.uniride.databinding.ItemTravelRequestBinding
import com.example.uniride.domain.model.TravelRequest
import com.example.uniride.domain.model.TravelRequestStatus

class TravelRequestAdapter(
    private val items: List<TravelRequest>
) : RecyclerView.Adapter<TravelRequestAdapter.RequestViewHolder>() {

    inner class RequestViewHolder(
        val binding: ItemTravelRequestBinding
    ) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RequestViewHolder {
        val binding = ItemTravelRequestBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return RequestViewHolder(binding)
    }

    override fun onBindViewHolder(holder: RequestViewHolder, position: Int) {
        val item = items[position]
        val travel = item.travelOption
        val context = holder.binding.root.context

        holder.binding.apply {
            tvDestination.text = "Destino: ${travel.destination}"

            // Formato de fecha y hora
            val formattedDate = item.requestDate.toLocalDate().toString()  // ej: "2025-05-05"
            val formattedTime = travel.departureTime  // ej: "17:30"
            tvDatetime.text = "$formattedDate - $formattedTime"

            tvPrice.text = "$${travel.price}"

            ivStatus.setImageResource(item.status.iconRes)
            tvStatus.text = item.status.label
            tvStatus.setTextColor(ContextCompat.getColor(context, item.status.colorRes))

            // para el histotial de viajes terminados
            if (item.status is TravelRequestStatus.Finished) {
                holder.itemView.alpha = 0.8f
                holder.itemView.isClickable = false
                holder.itemView.setBackgroundResource(R.drawable.bg_item_finished)
            }


        }
    }

    override fun getItemCount(): Int = items.size
}
