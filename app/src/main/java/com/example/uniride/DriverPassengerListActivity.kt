package com.example.uniride

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RatingBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.uniride.databinding.ActivityDriverPassengerListBinding

class DriverPassengersListActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDriverPassengerListBinding
    //Provitional example
    private val passengersList = listOf(
        Passenger("Joseph Guerra", false),
        Passenger("Laura Guevara", false),
        Passenger("Esteven Hernandez", true)
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDriverPassengerListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnBack.setOnClickListener {
            finish()
        }

        binding.btnSettings.setOnClickListener {
            Toast.makeText(this, "Settings", Toast.LENGTH_SHORT).show()
        }

        loadPassengersList()
    }

    //Method to load the passenger list
    private fun loadPassengersList() {
        binding.passengersContainer.removeAllViews()

        if (passengersList.isEmpty()) {
            val noPassengersText = TextView(this).apply {
                text = "No hay pasajeros en esta ruta"
                textAlignment = View.TEXT_ALIGNMENT_CENTER
                setPadding(0, 50, 0, 0)
            }
            binding.passengersContainer.addView(noPassengersText)
            return
        }

        for (passenger in passengersList) {
            addPassengerView(passenger)
        }
    }

    //Method to add a passenger to the view in the xml
    private fun addPassengerView(passenger: Passenger) {
        val passengerView = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            setPadding(0, 24, 0, 24)
        }

        val headerRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            gravity = android.view.Gravity.CENTER_VERTICAL
        }

        if (passenger.useIcon) {
            val icon = ImageView(this).apply {
                layoutParams = LinearLayout.LayoutParams(120, 120)
                setImageResource(R.drawable.ic_passenger)
                background = ContextCompat.getDrawable(context, R.drawable.round_avatar)
                setPadding(6, 6, 6, 6)
            }
            headerRow.addView(icon)
        } else {
            val avatar = ImageView(this).apply {
                layoutParams = LinearLayout.LayoutParams(120, 120)
                setImageResource(R.drawable.ic_passenger)
                background = ContextCompat.getDrawable(context, R.drawable.round_avatar)
                setPadding(6, 6, 6, 6)
            }
            headerRow.addView(avatar)
        }

        val nameText = TextView(this).apply {
            text = passenger.name
            textSize = 16f
            setTextColor(ContextCompat.getColor(context, android.R.color.black))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                marginStart = 16
            }
        }
        headerRow.addView(nameText)
        passengerView.addView(headerRow)

        val ratingBar = RatingBar(this, null, android.R.attr.ratingBarStyleSmall).apply {
            numStars = 5
            rating = 0f
            //isIndicator = true
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = 12
            }
        }
        passengerView.addView(ratingBar)

        //Add the message button for every passenger
        val messageButton = Button(this).apply {
            text = "Mensaje"
            setTextColor(ContextCompat.getColor(context, android.R.color.black))
            setBackgroundColor(ContextCompat.getColor(context, android.R.color.holo_blue_light))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                height = 120
                topMargin = 12
            }
            setOnClickListener {
                val intent = Intent(context, MessageActivity::class.java)
                context.startActivity(intent)
            }
        }
        passengerView.addView(messageButton)

        if (passenger != passengersList.last()) {
            val divider = View(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    3
                ).apply {
                    topMargin = 24
                }
                setBackgroundColor(ContextCompat.getColor(context, android.R.color.darker_gray))
            }
            passengerView.addView(divider)
        }

        binding.passengersContainer.addView(passengerView)
    }

    //Provisional Data Class
    data class Passenger(val name: String, val useIcon: Boolean)
}