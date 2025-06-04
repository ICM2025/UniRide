package com.example.uniride.domain.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.RatingBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.uniride.R
import com.example.uniride.domain.model.front.PassengerRatingItem

class PassengerRatingAdapter(
    private val passengers: List<PassengerRatingItem>
) : RecyclerView.Adapter<PassengerRatingAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imgProfile: ImageView = view.findViewById(R.id.imgProfile)
        val tvPassengerName: TextView = view.findViewById(R.id.tvPassengerName)
        val ratingBar: RatingBar = view.findViewById(R.id.ratingBar)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_passenger_rating, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = passengers[position]

        holder.tvPassengerName.text = item.name
        holder.ratingBar.rating = item.stars.toFloat()

        holder.ratingBar.setOnRatingBarChangeListener { _, rating, _ ->
            passengers[position].stars = rating.toInt()
        }

        if (!item.profileUrl.isNullOrEmpty()) {
            Glide.with(holder.itemView.context)
                .load(item.profileUrl)
                .placeholder(R.drawable.ic_profile)
                .error(R.drawable.ic_profile)
                .into(holder.imgProfile)
        } else {
            holder.imgProfile.setImageResource(R.drawable.ic_profile)
        }
    }

    override fun getItemCount(): Int = passengers.size

    fun getRatings(): List<PassengerRatingItem> {
        return passengers
    }
}
