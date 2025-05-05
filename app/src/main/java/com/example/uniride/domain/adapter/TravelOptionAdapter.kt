package com.example.uniride.domain.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.uniride.databinding.ItemTravelOptionBinding
import com.example.uniride.domain.model.TravelOption

class TravelOptionAdapter(
    private val options: List<TravelOption>,
    private val onItemClick: (TravelOption) -> Unit
) : RecyclerView.Adapter<TravelOptionAdapter.ViewHolder>() {

    inner class ViewHolder(private val binding: ItemTravelOptionBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(option: TravelOption) {
            binding.tvDestination.text = option.destination
            binding.tvDepartureTime.text = option.departureTime
            binding.tvPrice.text = "$${option.price}"
            binding.ivIcon.setImageResource(option.drawableResId)

            binding.root.setOnClickListener {
                onItemClick(option)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemTravelOptionBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(options[position])
    }

    override fun getItemCount(): Int = options.size
}
