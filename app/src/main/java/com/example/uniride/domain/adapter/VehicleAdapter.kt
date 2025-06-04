package com.example.uniride.domain.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.uniride.R
import com.example.uniride.domain.model.Car

class VehicleAdapter(
    private val onItemClick: (Car) -> Unit,
    private val onTakePhotoClick: (Car) -> Unit,
    private val onDeleteClick: ((Car) -> Unit)? = null
) : ListAdapter<Car, VehicleAdapter.VehicleViewHolder>(VehicleDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VehicleViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_vehicle, parent, false)
        return VehicleViewHolder(view)
    }

    override fun onBindViewHolder(holder: VehicleViewHolder, position: Int) {
        val car = getItem(position)
        holder.bind(car)
    }

    inner class VehicleViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvBrandModel: TextView = itemView.findViewById(R.id.tv_brand_model)
        private val tvYear: TextView = itemView.findViewById(R.id.tv_year)
        private val tvColor: TextView = itemView.findViewById(R.id.tv_color)
        private val tvLicensePlate: TextView = itemView.findViewById(R.id.tv_license_plate)
        private val ivVehicle: ImageView = itemView.findViewById(R.id.iv_vehicle_image)
        private val btnAddPhoto: View = itemView.findViewById(R.id.btn_add_photo)
        private val btnEdit: View = itemView.findViewById(R.id.btn_edit)

        fun bind(car: Car) {
            // Combinar marca y modelo en un solo TextView
            tvBrandModel.text = "${car.brand} ${car.model}"
            tvYear.text = car.year.toString()
            tvColor.text = car.color
            tvLicensePlate.text = car.licensePlate

            // Cargar imagen si existe
            if (car.images.isNotEmpty()) {
                Glide.with(itemView.context)
                    .load(car.images[0])
                    .placeholder(R.drawable.ic_vehicle_placeholder)
                    .error(R.drawable.ic_vehicle_placeholder)
                    .into(ivVehicle)
            } else {
                // Usar un drawable por defecto o ocultar la imagen
                try {
                    ivVehicle.setImageResource(R.drawable.ic_vehicle_placeholder)
                } catch (e: Exception) {
                    // Si no existe el drawable, usar un color de fondo
                    ivVehicle.setBackgroundColor(android.graphics.Color.LTGRAY)
                }
            }

            // Click listeners
            itemView.setOnClickListener {
                onItemClick(car)
            }

            btnAddPhoto.setOnClickListener {
                onTakePhotoClick(car)
            }

            btnEdit.setOnClickListener {
                onItemClick(car)
            }

            // Si hay callback de eliminación, usar click largo en el item
            itemView.setOnLongClickListener {
                onDeleteClick?.invoke(car)
                true
            }
        }
    }

    class VehicleDiffCallback : DiffUtil.ItemCallback<Car>() {
        override fun areItemsTheSame(oldItem: Car, newItem: Car): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Car, newItem: Car): Boolean {
            return oldItem == newItem
        }
    }
}