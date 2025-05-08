package com.example.uniride.domain.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.uniride.R
import com.example.uniride.databinding.ItemTravelRequestBinding
import com.example.uniride.domain.model.TravelRequest
import com.example.uniride.domain.model.TravelRequestStatus

class TravelRequestAdapter(
    private val items: List<TravelRequest>,
    private val onClick: (TravelRequest) -> Unit
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

            val formattedDate = item.requestDate.toLocalDate().toString()
            val formattedTime = travel.departureTime
            tvDatetime.text = "$formattedDate - $formattedTime"

            tvPrice.text = "$${travel.price}"

            ivStatus.setImageResource(item.status.iconRes)
            tvStatus.text = item.status.label
            tvStatus.setTextColor(ContextCompat.getColor(context, item.status.colorRes))

            if (item.status is TravelRequestStatus.Finished) {
                holder.itemView.alpha = 0.8f
                holder.itemView.isClickable = false
                holder.itemView.setBackgroundResource(R.drawable.bg_item_finished)
                // desactiva clicks
                holder.itemView.setOnClickListener(null)
            } else {
                holder.itemView.setOnClickListener {
                    //ejecuta la función lambda que se pasa por parámetro
                    onClick(item)
                }
            }
        }
    }

    override fun getItemCount(): Int = items.size
}
