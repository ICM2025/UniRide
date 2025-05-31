package com.example.uniride.domain.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.uniride.R
import com.example.uniride.domain.model.Mensaje

class MensajesAdapter(private val mensajes: List<Mensaje>, private val miUid: String) :
    RecyclerView.Adapter<MensajesAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val textoMensaje: TextView = view.findViewById(R.id.textoMensaje)
        val contenedor: LinearLayout = view.findViewById(R.id.contenedorMensaje)
    }

    override fun getItemViewType(position: Int): Int {
        return if (mensajes[position].emisor == miUid) 1 else 0
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val layout = if (viewType == 1)
            R.layout.item_mensaje_mio
        else
            R.layout.item_mensaje_otro

        val view = LayoutInflater.from(parent.context).inflate(layout, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.textoMensaje.text = mensajes[position].texto
    }

    override fun getItemCount(): Int = mensajes.size
}
