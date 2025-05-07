package com.example.uniride.domain.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.uniride.databinding.ItemTripRequestDriverBinding
import com.example.uniride.domain.model.PassengerRequest
import com.example.uniride.domain.model.PassengerRequestStatus

class PassengerRequestsAdapter(
    private val items: List<PassengerRequest>,
    private val onAccept: (PassengerRequest) -> Unit,
    private val onReject: (PassengerRequest) -> Unit,
    private val onClick: (PassengerRequest) -> Unit
) : RecyclerView.Adapter<PassengerRequestsAdapter.RequestViewHolder>() {

    inner class RequestViewHolder(val binding: ItemTripRequestDriverBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RequestViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ItemTripRequestDriverBinding.inflate(inflater, parent, false)
        return RequestViewHolder(binding)
    }

    override fun onBindViewHolder(holder: RequestViewHolder, position: Int) {
        val item = items[position]
        val status = item.status
        val context = holder.itemView.context

        holder.binding.apply {
            tvPassengerName.text = item.passengerName
            tvDestination.text = "Destino: ${item.destination}"

            // Estado visual
            tvStatus.text = status.label
            tvStatus.setTextColor(ContextCompat.getColor(context, status.color))
            ivStatus.setImageResource(status.icon)

            // botones solo si está pendiente por responder
            if (status is PassengerRequestStatus.Pending) {
                //aplica directamente al linear que contiene a los botones
                actionButtons.visibility = View.VISIBLE
                btnAccept.setOnClickListener { onAccept(item) }
                btnReject.setOnClickListener { onReject(item) }
            } else {
                actionButtons.visibility = View.GONE
            }
        }
        holder.itemView.setOnClickListener {
            onClick(item)
        }

    }

    override fun getItemCount(): Int = items.size
}
