package com.example.uniride.adapters


import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.TextView
import com.example.uniride.model.Solicitud
import com.example.uniride.R

class SolicitudAdapter(context: Context, private val solicitudes: List<Solicitud>) :
    ArrayAdapter<Solicitud>(context, 0, solicitudes) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: LayoutInflater.from(context).inflate(R.layout.item_solicitud, parent, false)

        val solicitud = solicitudes[position]

        val carIcon = view.findViewById<ImageView>(R.id.carIcon1)
        val title = view.findViewById<TextView>(R.id.requestTitle1)
        val details = view.findViewById<TextView>(R.id.requestDetails1)
        val price = view.findViewById<TextView>(R.id.requestPrice1)

        carIcon.setImageResource(R.drawable.ic_driver)
        title.text = solicitud.titulo
        details.text = solicitud.detalles
        price.text = solicitud.precio

        return view
    }
}
