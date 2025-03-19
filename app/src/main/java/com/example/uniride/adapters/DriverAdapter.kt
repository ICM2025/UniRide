package com.example.uniride

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.BaseAdapter
import com.example.uniride.model.Driver
import com.example.uniride.R


class DriverAdapter(private val context: Context, private val drivers: List<Driver>) : BaseAdapter() {

    override fun getCount(): Int = drivers.size

    override fun getItem(position: Int): Any = drivers[position]

    override fun getItemId(position: Int): Long = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val view = convertView ?: LayoutInflater.from(context).inflate(R.layout.driver_item, parent, false)
        val driver = drivers[position]

        val driverImage = view.findViewById<ImageView>(R.id.imageView2)
        val driverName = view.findViewById<TextView>(R.id.driverName)

        driverImage.setImageResource(driver.imageResId)
        driverName.text = driver.name

        view.setOnClickListener {
            val intent = Intent(context, DriverProfileActivity::class.java).apply {
                putExtra("DRIVER_NAME", driver.name)
                putExtra("DRIVER_EMAIL", driver.email)
                putExtra("DRIVER_IMAGE", driver.imageResId)
            }
            context.startActivity(intent)
        }

        return view
    }


}