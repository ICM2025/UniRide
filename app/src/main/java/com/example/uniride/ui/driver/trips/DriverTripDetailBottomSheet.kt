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
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.navigation.fragment.findNavController
import com.example.uniride.R
import com.example.uniride.domain.model.DriverTripItem
import com.example.uniride.domain.model.Route
import com.example.uniride.domain.model.StopInRoute
import com.example.uniride.domain.model.Trip
import com.example.uniride.ui.driver.publish.PublishTripFlowActivity
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.button.MaterialButton
import com.google.firebase.firestore.FirebaseFirestore
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter

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

        val info = trip.tripInformation

        // Vistas
        val tvRoute = view.findViewById<TextView>(R.id.tv_route)
        val tvVehicleUsed = view.findViewById<TextView>(R.id.tv_vehicle_used)
        val tvDateTime = view.findViewById<TextView>(R.id.tv_date_time)
        val tvPrice = view.findViewById<TextView>(R.id.tv_price)
        val tvSeatsInfo = view.findViewById<TextView>(R.id.tv_seats_info)
        val progressBar = view.findViewById<ProgressBar>(R.id.progress_seats)
        val tvSummary = view.findViewById<TextView>(R.id.tv_summary)
        val ivVehicleIcon = view.findViewById<ImageView>(R.id.iv_vehicle_icon)

        // Mostrar datos
        tvRoute.text = "Ruta: ${info.origin} → ${info.destination}"
        tvVehicleUsed.text = "Vehículo: ${info.carName} (${info.carPlate})"
        tvDateTime.text = "${info.travelDate} · ${info.departureTime}"
        tvPrice.text = "Precio: $${info.price.toInt()}"
        ivVehicleIcon.setImageResource(info.carIcon)

        // Cupos
        val totalSeats = info.availableSeats
        val occupiedSeats = trip.acceptedCount
        val progress = if (totalSeats > 0) (occupiedSeats * 100) / totalSeats else 0

        tvSeatsInfo.text = "$occupiedSeats de $totalSeats cupos ocupados"
        progressBar.progress = progress
        tvSummary.text = "${trip.acceptedCount} pasajeros aceptados · ${trip.pendingCount} pendientes"



        setupButtons(view)
        updateStartButtonState(view)
    }


    private fun updateStartButtonState(view: View) {
        val btnStartTrip = view.findViewById<MaterialButton>(R.id.btn_start_trip)
        val minutesToDeparture = calculateMinutesToDeparture()

        when {
            // Ya está activo
            isActiveTrip() -> {
                btnStartTrip.text = "Viaje en curso"
                btnStartTrip.isEnabled = false
                btnStartTrip.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.status_accepted))
            }
            // Puede iniciarse (menos de 10 minutos)
            minutesToDeparture in 0..10 -> {
                btnStartTrip.text = "Iniciar viaje"
                btnStartTrip.isEnabled = true
                btnStartTrip.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.status_pending))
            }
            // Muy temprano para iniciar
            minutesToDeparture > 10 -> {
                btnStartTrip.text = "Inicia en ${minutesToDeparture} min"
                btnStartTrip.isEnabled = false
                btnStartTrip.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.status_pending))
            }
            // Viaje vencido
            else -> {
                btnStartTrip.text = "Viaje vencido"
                btnStartTrip.isEnabled = false
                btnStartTrip.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.status_rejected))
            }
        }
    }

    private fun calculateMinutesToDeparture(): Long {
        return try {
            val time = LocalTime.parse(trip.tripInformation.departureTime, DateTimeFormatter.ofPattern("HH:mm"))
            val departureDateTime = LocalDateTime.of(trip.tripInformation.travelDate, time)
            val now = LocalDateTime.now()

            val duration = java.time.Duration.between(now, departureDateTime)
            duration.toMinutes()
        } catch (e: Exception) {
            Long.MAX_VALUE
        }
    }

    private fun isActiveTrip(): Boolean {
        val sharedPreferences = requireContext().getSharedPreferences("route_data", Context.MODE_PRIVATE)
        val activeTripId = sharedPreferences.getString("ACTIVE_TRIP_ID", "")
        return activeTripId == trip.tripId
    }

    private fun setupButtons(view: View) {
        view.findViewById<MaterialButton>(R.id.btn_start_trip).setOnClickListener {
            val minutesToDeparture = calculateMinutesToDeparture()

            when {
                isActiveTrip() -> {
                    Toast.makeText(requireContext(), "Este viaje ya está en curso", Toast.LENGTH_SHORT).show()
                }
                minutesToDeparture in 0..10 -> {
                    startTrip()
                }
                minutesToDeparture > 10 -> {
                    showEarlyStartDialog(minutesToDeparture)
                }
                else -> {
                    Toast.makeText(requireContext(), "Este viaje ya no se puede iniciar", Toast.LENGTH_SHORT).show()
                }
            }
        }

        view.findViewById<MaterialButton>(R.id.btn_edit_trip).setOnClickListener {
            editTrip()
        }

        view.findViewById<MaterialButton>(R.id.btn_cancel_trip).setOnClickListener {
            showCancelConfirmation()
        }
    }

    private fun showEarlyStartDialog(minutesToDeparture: Long) {
        AlertDialog.Builder(requireContext())
            .setTitle("Iniciar viaje temprano")
            .setMessage("El viaje está programado para iniciar en $minutesToDeparture minutos. ¿Estás seguro de que quieres iniciarlo ahora?")
            .setPositiveButton("Sí, iniciar ahora") { _, _ ->
                startTrip()
            }
            .setNegativeButton("Esperar") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun startTrip() {
        val sharedPreferences = requireContext().getSharedPreferences("route_data", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()

        val tripId = trip.tripId

        // Verificar si ya hay un viaje activo
        val currentActiveTripId = sharedPreferences.getString("ACTIVE_TRIP_ID", "")
        if (!currentActiveTripId.isNullOrEmpty() && currentActiveTripId != tripId) {
            AlertDialog.Builder(requireContext())
                .setTitle("Viaje activo")
                .setMessage("Ya tienes un viaje en curso. ¿Quieres finalizarlo e iniciar este nuevo viaje?")
                .setPositiveButton("Sí, cambiar viaje") { _, _ ->
                    proceedToStartTrip(editor, tripId)
                }
                .setNegativeButton("Cancelar", null)
                .show()
            return
        }

        proceedToStartTrip(editor, tripId)
    }

    private fun proceedToStartTrip(editor: android.content.SharedPreferences.Editor, tripId: String) {
        val sharedPreferences = requireContext().getSharedPreferences("route_data", Context.MODE_PRIVATE)

        // Marcar esta ruta como activa
        editor.putBoolean("HAS_ACTIVE_ROUTE", true)
        editor.putString("ACTIVE_TRIP_ID", tripId)

        // NUEVO: Obtener datos del viaje desde Firebase usando el tripId
        val db = FirebaseFirestore.getInstance()
        db.collection("Trips").document(tripId)
            .get()
            .addOnSuccessListener { tripDoc ->
                val trip = tripDoc.toObject(Trip::class.java)
                if (trip != null) {
                    // Obtener datos de la ruta
                    db.collection("Routes").document(trip.idRoute)
                        .get()
                        .addOnSuccessListener { routeDoc ->
                            val route = routeDoc.toObject(Route::class.java)
                            if (route != null) {
                                // Obtener nombres de origen y destino
                                fetchStopNames(route.idOrigin, route.idDestination) { origin, destination ->
                                    // Guardar datos de la ruta activa
                                    editor.putString("ROUTE_ORIGIN", origin)
                                    editor.putString("ROUTE_DESTINATION", destination)

                                    // Obtener y guardar paradas intermedias
                                    fetchIntermediateStops(trip.idRoute) { stops ->
                                        editor.putInt("ROUTE_STOPS_COUNT", stops.size)
                                        stops.forEachIndexed { index, stopName ->
                                            editor.putString("ROUTE_STOP_$index", stopName)
                                        }

                                        editor.putLong("ROUTE_DATETIME", System.currentTimeMillis())
                                        editor.putString("TRIP_${tripId}_ACTUAL_START_TIME",
                                            LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy")))

                                        editor.apply()

                                        showSuccessDialog()
                                    }
                                }
                            }
                        }
                }
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Error al obtener datos del viaje", Toast.LENGTH_SHORT).show()
            }
    }

    private fun fetchStopNames(originId: String, destinationId: String, onResult: (String, String) -> Unit) {
        val db = FirebaseFirestore.getInstance()
        db.collection("Stops").document(originId).get().addOnSuccessListener { originSnap ->
            val originName = originSnap.getString("name") ?: "Origen"

            db.collection("Stops").document(destinationId).get().addOnSuccessListener { destSnap ->
                val destName = destSnap.getString("name") ?: "Destino"
                onResult(originName, destName)
            }
        }
    }

    // NUEVO: Método para obtener paradas intermedias
    private fun fetchIntermediateStops(routeId: String, onResult: (List<String>) -> Unit) {
        val db = FirebaseFirestore.getInstance()
        db.collection("StopsInRoute")
            .whereEqualTo("idRoute", routeId)
            .orderBy("position")
            .get()
            .addOnSuccessListener { snapshot ->
                val stopIds = snapshot.documents.mapNotNull {
                    it.toObject(StopInRoute::class.java)?.idStop
                }

                if (stopIds.isEmpty()) {
                    onResult(emptyList())
                    return@addOnSuccessListener
                }

                val stopNames = mutableListOf<String>()
                var completedCount = 0

                stopIds.forEach { stopId ->
                    db.collection("Stops").document(stopId).get()
                        .addOnSuccessListener { stopDoc ->
                            val stopName = stopDoc.getString("name") ?: "Parada"
                            stopNames.add(stopName)
                            completedCount++

                            if (completedCount == stopIds.size) {
                                onResult(stopNames)
                            }
                        }
                        .addOnFailureListener {
                            completedCount++
                            if (completedCount == stopIds.size) {
                                onResult(stopNames)
                            }
                        }
                }
            }
            .addOnFailureListener {
                onResult(emptyList())
            }
    }

    // NUEVO: Método para mostrar diálogo de éxito
    private fun showSuccessDialog() {
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
                (requireActivity() as? androidx.navigation.NavController)?.let { navController ->
                    navController.navigate(R.id.driverHomeFragment)
                }
            }
        }, 1500)
    }

    private fun editTrip() {
        // No permitir editar viajes activos
        if (isActiveTrip()) {
            Toast.makeText(requireContext(), "No puedes editar un viaje que está en curso", Toast.LENGTH_SHORT).show()
            return
        }

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
        val dateTime = sharedPreferences.getLong("TRIP_${tripId}_DATETIME", System.currentTimeMillis()) // Nueva línea

        editor.putString("EDIT_ROUTE_ORIGIN", origin)
        editor.putString("EDIT_ROUTE_DESTINATION", destination)
        editor.putInt("EDIT_ROUTE_STOPS_COUNT", stopsCount)
        editor.putLong("EDIT_ROUTE_DATETIME", dateTime) // Nueva línea

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
        // No permitir cancelar viajes activos sin más contexto
        if (isActiveTrip()) {
            AlertDialog.Builder(requireContext())
                .setTitle("Finalizar Viaje Activo")
                .setMessage("Este viaje está en curso. ¿Quieres finalizarlo? Los pasajeros serán notificados.")
                .setPositiveButton("Sí, finalizar") { _, _ ->
                    cancelTrip()
                }
                .setNegativeButton("Cancelar", null)
                .show()
        } else {
            AlertDialog.Builder(requireContext())
                .setTitle("Cancelar Viaje")
                .setMessage("¿Estás seguro de que quieres cancelar este viaje? Esta acción no se puede deshacer.")
                .setPositiveButton("Sí, cancelar") { _, _ ->
                    cancelTrip()
                }
                .setNegativeButton("No", null)
                .show()
        }
    }

    private fun cancelTrip() {
        val sharedPreferences = requireContext().getSharedPreferences("route_data", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()

        val tripId = trip.tripId

        // Eliminar todos los datos relacionados con este viaje
        editor.remove("TRIP_${tripId}_ORIGIN")
        editor.remove("TRIP_${tripId}_DESTINATION")
        editor.remove("TRIP_${tripId}_STOPS_COUNT")
        editor.remove("TRIP_${tripId}_DEPARTURE_TIME")
        editor.remove("TRIP_${tripId}_DATE")
        editor.remove("TRIP_${tripId}_ACTUAL_START_TIME")

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

        val message = if (isActiveTrip()) "Viaje finalizado exitosamente" else "Viaje cancelado exitosamente"
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()

        // Notificar que el viaje se actualizó
        onTripUpdated?.invoke()

        dismiss()
    }
}