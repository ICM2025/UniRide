package com.example.uniride

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import com.example.uniride.model.Driver


class DriverAdapter(private val context: Context, private val drivers: List<Driver>) :
    ArrayAdapter<Driver>(context, 0, drivers) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        var itemView = convertView
        if (itemView == null) {
            itemView = LayoutInflater.from(context).inflate(R.layout.item_driver, parent, false)
        }

        val currentDriver = drivers[position]

        val imageView = itemView?.findViewById<ImageView>(R.id.driverImageView)
        val nameText = itemView?.findViewById<TextView>(R.id.driverNameText)
        val emailText = itemView?.findViewById<TextView>(R.id.driverEmailText)
        val verRutaBtn = itemView?.findViewById<Button>(R.id.btnVerRuta)

        imageView?.setImageResource(currentDriver.imageResId)
        nameText?.text = currentDriver.name
        emailText?.text = currentDriver.email

        verRutaBtn?.setOnClickListener {
            Toast.makeText(context, "Ver ruta de ${currentDriver.name}", Toast.LENGTH_SHORT).show()
            val intent = Intent(context, DriverRouteInProgressActivity::class.java)
            intent.putExtra("DRIVER_NAME", currentDriver.name)
            context.startActivity(intent)
        }

        return itemView!!
    }
}