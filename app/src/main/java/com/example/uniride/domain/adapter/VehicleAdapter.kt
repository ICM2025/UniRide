package com.example.uniride.domain.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.uniride.R
import com.example.uniride.domain.model.Vehicle

class VehicleAdapter : RecyclerView.Adapter<VehicleAdapter.VehicleViewHolder>() {

    private val vehicles = mutableListOf<Vehicle>()

    inner class VehicleViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val modelBrand: TextView = itemView.findViewById(R.id.tv_model_brand)
        val plateYear: TextView = itemView.findViewById(R.id.tv_plate_year)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VehicleViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_vehicle, parent, false)
        return VehicleViewHolder(view)
    }

    override fun onBindViewHolder(holder: VehicleViewHolder, position: Int) {
        val vehicle = vehicles[position]
        holder.modelBrand.text = "${vehicle.brand} ${vehicle.model}"
        holder.plateYear.text = "Placa: ${vehicle.licensePlate} - Año: ${vehicle.year}"
    }

    override fun getItemCount(): Int = vehicles.size

    fun submitList(newList: List<Vehicle>) {
        vehicles.clear()
        vehicles.addAll(newList)
        notifyDataSetChanged()
    }
}
