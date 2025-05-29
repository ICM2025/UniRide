package com.example.uniride.domain.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.uniride.databinding.ItemTravelOptionBinding
import com.example.uniride.domain.model.front.TripPassengerDetail

class TripPassengerDetailAdapter(
    private val items: List<TripPassengerDetail>,
    private val onItemClick: (TripPassengerDetail) -> Unit
) : RecyclerView.Adapter<TripPassengerDetailAdapter.ViewHolder>() {

    inner class ViewHolder(private val binding: ItemTravelOptionBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: TripPassengerDetail) {
            val info = item.tripInformation

            binding.tvDestination.text = info.destination
            binding.tvDepartureTime.text = info.departureTime
            binding.tvPrice.text = "$${info.price}"
            binding.ivIcon.setImageResource(info.carIcon)

            binding.root.setOnClickListener {
                onItemClick(item)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ItemTravelOptionBinding.inflate(inflater, parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size
}
