package com.example.uniride.domain.adapter

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.uniride.R
import com.example.uniride.domain.model.Vehicle

class VehicleAdapter(
    private val onItemClick: (Vehicle) -> Unit = {},
    private val onTakePhotoClick: (Vehicle) -> Unit = {}
) : ListAdapter<Vehicle, VehicleAdapter.VehicleViewHolder>(VehicleDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VehicleViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_vehicle, parent, false)
        return VehicleViewHolder(view)
    }

    override fun onBindViewHolder(holder: VehicleViewHolder, position: Int) {
        val vehicle = getItem(position)
        holder.bind(vehicle, onItemClick, onTakePhotoClick)
    }

    class VehicleViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvBrandModel: TextView = itemView.findViewById(R.id.tv_brand_model)
        private val tvYear: TextView = itemView.findViewById(R.id.tv_year)
        private val tvColor: TextView = itemView.findViewById(R.id.tv_color)
        private val tvLicensePlate: TextView = itemView.findViewById(R.id.tv_license_plate)
        private val ivVehicleImage: ImageView = itemView.findViewById(R.id.iv_vehicle_image)
        private val btnAddPhoto: ImageButton = itemView.findViewById(R.id.btn_add_photo)
        private val btnEdit: ImageButton = itemView.findViewById(R.id.btn_edit)

        fun bind(
            vehicle: Vehicle,
            onItemClick: (Vehicle) -> Unit,
            onTakePhotoClick: (Vehicle) -> Unit
        ) {
            tvBrandModel.text = "${vehicle.brand} ${vehicle.model}"
            tvYear.text = vehicle.year.toString()
            tvColor.text = vehicle.color
            tvLicensePlate.text = vehicle.licensePlate

            // Cargar imagen si existe
            if (vehicle.imageUrls.isNotEmpty()) {
                try {
                    val uri = Uri.parse(vehicle.imageUrls[0])
                    ivVehicleImage.setImageURI(uri)
                    ivVehicleImage.visibility = View.VISIBLE
                } catch (e: Exception) {
                    e.printStackTrace()
                    ivVehicleImage.visibility = View.GONE
                }
            } else {
                ivVehicleImage.visibility = View.GONE
            }

            itemView.setOnClickListener { onItemClick(vehicle) }
            btnEdit.setOnClickListener { onItemClick(vehicle) }
            btnAddPhoto.setOnClickListener { onTakePhotoClick(vehicle) }
        }
    }

    class VehicleDiffCallback : DiffUtil.ItemCallback<Vehicle>() {
        override fun areItemsTheSame(oldItem: Vehicle, newItem: Vehicle): Boolean {
            return oldItem.licensePlate == newItem.licensePlate
        }

        override fun areContentsTheSame(oldItem: Vehicle, newItem: Vehicle): Boolean {
            return oldItem == newItem
        }
    }
}