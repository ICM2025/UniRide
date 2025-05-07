package com.example.uniride.ui.driver.trips


import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.uniride.R
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.button.MaterialButton
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import com.example.uniride.domain.model.DriverTripItem

class DriverTripDetailBottomSheet(
    private val trip: DriverTripItem) : BottomSheetDialogFragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.item_travel_detail_driver, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        val progressBar = view.findViewById<ProgressBar>(R.id.progress_seats)
        val tvSeatsInfo = view.findViewById<TextView>(R.id.tv_seats_info)
        val tvSummary = view.findViewById<TextView>(R.id.tv_summary)

        //por ahora los datos quemados
        tvSeatsInfo.text = "3 de 4 cupos ocupados"
        progressBar.progress = 75 // 3/4
        tvSummary.text = "3 pasajeros aceptados · 2 pendientes"

        view.findViewById<MaterialButton>(R.id.btn_start_trip).setOnClickListener {
            val sharedPreferences = requireContext().getSharedPreferences("route_data", Context.MODE_PRIVATE)
            val editor = sharedPreferences.edit()

            // Marcar esta ruta como activa
            editor.putBoolean("HAS_ACTIVE_ROUTE", true)

            // Guardar los datos de la ruta activa para que la use el mapa
            val tripId = trip.tripId

            // Copiar los datos de este viaje específico a las claves que usa el mapa
            editor.putString("ROUTE_ORIGIN", sharedPreferences.getString("TRIP_${tripId}_ORIGIN", ""))
            editor.putString("ROUTE_DESTINATION", sharedPreferences.getString("TRIP_${tripId}_DESTINATION", ""))
            editor.putInt("ROUTE_STOPS_COUNT", sharedPreferences.getInt("TRIP_${tripId}_STOPS_COUNT", 0))

            // Copiar cada parada
            val stopsCount = sharedPreferences.getInt("TRIP_${tripId}_STOPS_COUNT", 0)
            for (i in 0 until stopsCount) {
                editor.putString("ROUTE_STOP_$i", sharedPreferences.getString("TRIP_${tripId}_STOP_$i", ""))
            }

            editor.apply()

            Toast.makeText(requireContext(), "Viaje iniciado", Toast.LENGTH_SHORT).show()
            dismiss()
        }

        view.findViewById<MaterialButton>(R.id.btn_edit_trip).setOnClickListener {
            Toast.makeText(requireContext(), "Editar viaje", Toast.LENGTH_SHORT).show()
            dismiss()
        }

        view.findViewById<MaterialButton>(R.id.btn_cancel_trip).setOnClickListener {
            Toast.makeText(requireContext(), "Viaje cancelado", Toast.LENGTH_SHORT).show()
            dismiss()
        }
    }
}
