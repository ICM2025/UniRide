package com.example.uniride.ui.driver.trips


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

        // Botones
        view.findViewById<MaterialButton>(R.id.btn_edit_trip).setOnClickListener {
            Toast.makeText(requireContext(), "Editar viaje", Toast.LENGTH_SHORT).show()
            // se cierra bottom sheet y se debería navegar
            dismiss()
        }

        view.findViewById<MaterialButton>(R.id.btn_cancel_trip).setOnClickListener {
            Toast.makeText(requireContext(), "Viaje cancelado", Toast.LENGTH_SHORT).show()
            // se cierra bottom sheet y se debería navegar
            dismiss()
        }
    }
}
