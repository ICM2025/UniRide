package com.example.uniride.ui.driver.trips

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.navigation.fragment.findNavController
import com.example.uniride.R
import com.example.uniride.domain.model.DriverTripItem
import com.example.uniride.ui.driver.publish.PublishTripFlowActivity
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.button.MaterialButton

class DriverTripDetailBottomSheet(
    private val trip: DriverTripItem,
    private val onTripUpdated: (() -> Unit)? = null
) : BottomSheetDialogFragment() {

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

        // Mostrar información real del viaje
        val totalSeats = trip.travelOption.availableSeats
        val occupiedSeats = trip.acceptedCount
        val progress = if (totalSeats > 0) (occupiedSeats * 100) / totalSeats else 0

        tvSeatsInfo.text = "$occupiedSeats de $totalSeats cupos ocupados"
        progressBar.progress = progress
        tvSummary.text = "${trip.acceptedCount} pasajeros aceptados · ${trip.pendingCount} pendientes"

        setupButtons(view)
    }

    private fun setupButtons(view: View) {
        view.findViewById<MaterialButton>(R.id.btn_start_trip).setOnClickListener {
            startTrip()
        }

        view.findViewById<MaterialButton>(R.id.btn_edit_trip).setOnClickListener {
            editTrip()
        }

        view.findViewById<MaterialButton>(R.id.btn_cancel_trip).setOnClickListener {
            showCancelConfirmation()
        }
    }

    private fun startTrip() {
        val sharedPreferences = requireContext().getSharedPreferences("route_data", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()

        val tripId = trip.tripId

        // Marcar esta ruta como activa
        editor.putBoolean("HAS_ACTIVE_ROUTE", true)
        editor.putString("ACTIVE_TRIP_ID", tripId)

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

        // Mostrar diálogo de confirmación
        val dialogView = layoutInflater.inflate(R.layout.dialog_success_request, null)
        dialogView.findViewById<TextView>(R.id.tv_success).text = "¡Viaje iniciado!"
        dialogView.findViewById<TextView>(R.id.tv_secondary).text = "Tu viaje ha comenzado exitosamente"

        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .create()

        dialog.window?.setDimAmount(0.75f)
        dialog.show()

        Handler(Looper.getMainLooper()).postDelayed({
            dialog.dismiss()
            dismiss()

            // Navegar al DriverHomeFragment
            try {
                parentFragment?.findNavController()?.navigate(R.id.nav_home)
            } catch (e: Exception) {
                // Si no funciona la navegación, intentar con el activity
                (requireActivity() as? androidx.navigation.NavController)?.let { navController ->
                    navController.navigate(R.id.driverHomeFragment)
                }
            }
        }, 1500)
    }

    private fun editTrip() {
        val sharedPreferences = requireContext().getSharedPreferences("route_data", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        val tripId = trip.tripId

        // Preparar los datos para edición - usar las claves que el PublishRouteFragment espera
        editor.putString("EDITING_TRIP_ID", tripId)
        editor.putBoolean("IS_EDITING_TRIP", true)

        // Copiar los datos del viaje que se va a editar a las claves temporales correctas
        val origin = sharedPreferences.getString("TRIP_${tripId}_ORIGIN", "") ?: ""
        val destination = sharedPreferences.getString("TRIP_${tripId}_DESTINATION", "") ?: ""
        val stopsCount = sharedPreferences.getInt("TRIP_${tripId}_STOPS_COUNT", 0)

        editor.putString("EDIT_ROUTE_ORIGIN", origin)
        editor.putString("EDIT_ROUTE_DESTINATION", destination)
        editor.putInt("EDIT_ROUTE_STOPS_COUNT", stopsCount)

        // Copiar todas las paradas
        for (i in 0 until stopsCount) {
            val stop = sharedPreferences.getString("TRIP_${tripId}_STOP_$i", "") ?: ""
            editor.putString("EDIT_ROUTE_STOP_$i", stop)
        }

        editor.apply()

        // Crear intent con la información necesaria
        val intent = Intent(requireContext(), PublishTripFlowActivity::class.java)
        intent.putExtra("IS_EDITING", true)
        intent.putExtra("TRIP_ID", tripId)

        // Iniciar la actividad y cerrar el bottom sheet
        startActivity(intent)
        dismiss()
    }

    private fun showCancelConfirmation() {
        AlertDialog.Builder(requireContext())
            .setTitle("Cancelar Viaje")
            .setMessage("¿Estás seguro de que quieres cancelar este viaje? Esta acción no se puede deshacer.")
            .setPositiveButton("Sí, cancelar") { _, _ ->
                cancelTrip()
            }
            .setNegativeButton("No", null)
            .show()
    }

    private fun cancelTrip() {
        val sharedPreferences = requireContext().getSharedPreferences("route_data", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()

        val tripId = trip.tripId

        // Eliminar todos los datos relacionados con este viaje
        editor.remove("TRIP_${tripId}_ORIGIN")
        editor.remove("TRIP_${tripId}_DESTINATION")
        editor.remove("TRIP_${tripId}_STOPS_COUNT")

        // Eliminar las paradas
        val stopsCount = sharedPreferences.getInt("TRIP_${tripId}_STOPS_COUNT", 0)
        for (i in 0 until stopsCount) {
            editor.remove("TRIP_${tripId}_STOP_$i")
        }

        // Remover el ID del viaje de la lista de viajes guardados
        val tripIds = sharedPreferences.getStringSet("SAVED_TRIP_IDS", mutableSetOf())?.toMutableSet() ?: mutableSetOf()
        tripIds.remove(tripId)
        editor.putStringSet("SAVED_TRIP_IDS", tripIds)

        // Si este era el viaje activo, desactivarlo
        val activeTripId = sharedPreferences.getString("ACTIVE_TRIP_ID", "")
        if (activeTripId == tripId) {
            editor.putBoolean("HAS_ACTIVE_ROUTE", false)
            editor.remove("ACTIVE_TRIP_ID")
            editor.remove("ROUTE_ORIGIN")
            editor.remove("ROUTE_DESTINATION")
            editor.remove("ROUTE_STOPS_COUNT")

            for (i in 0 until stopsCount) {
                editor.remove("ROUTE_STOP_$i")
            }
        }

        // Si no hay más viajes, marcar que no hay rutas publicadas
        if (tripIds.isEmpty()) {
            editor.putBoolean("HAS_PUBLISHED_ROUTE", false)
        }

        // Limpiar datos temporales de edición si existen
        editor.remove("IS_EDITING_TRIP")
        editor.remove("EDITING_TRIP_ID")
        editor.remove("EDIT_ROUTE_ORIGIN")
        editor.remove("EDIT_ROUTE_DESTINATION")
        editor.remove("EDIT_ROUTE_STOPS_COUNT")

        val editStopsCount = sharedPreferences.getInt("EDIT_ROUTE_STOPS_COUNT", 0)
        for (i in 0 until editStopsCount) {
            editor.remove("EDIT_ROUTE_STOP_$i")
        }

        editor.apply()

        Toast.makeText(requireContext(), "Viaje cancelado exitosamente", Toast.LENGTH_SHORT).show()

        // Notificar que el viaje se actualizó
        onTripUpdated?.invoke()

        dismiss()
    }
}