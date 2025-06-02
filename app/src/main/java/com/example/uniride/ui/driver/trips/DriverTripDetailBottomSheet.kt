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
import com.google.firebase.auth.FirebaseAuth
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

        // CAMBIO: Verificar estado desde Firebase en lugar de SharedPreferences
        checkTripStatusFromFirebase { isActive, minutesToDeparture ->
            when {
                isActive -> {
                    btnStartTrip.text = "Viaje en curso"
                    btnStartTrip.isEnabled = false
                    btnStartTrip.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.status_accepted))
                }
                minutesToDeparture in 0..10 -> {
                    btnStartTrip.text = "Iniciar"
                    btnStartTrip.isEnabled = true
                    btnStartTrip.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.status_pending))
                }
                minutesToDeparture > 10 -> {
                    btnStartTrip.text = "${minutesToDeparture} min"
                    btnStartTrip.isEnabled = false
                    btnStartTrip.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.status_pending))
                }
                else -> {
                    btnStartTrip.text = "Vencido"
                    btnStartTrip.isEnabled = false
                    btnStartTrip.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.status_rejected))
                }
            }
        }
    }

    private fun checkTripStatusFromFirebase(callback: (isActive: Boolean, minutesToDeparture: Long) -> Unit) {
        val db = FirebaseFirestore.getInstance()

        db.collection("Trips").document(trip.tripId)
            .get()
            .addOnSuccessListener { tripDoc ->
                if (tripDoc.exists()) {
                    // Verificar si el viaje está activo basado en el campo 'status' de Firebase
                    val status = tripDoc.getString("status") ?: "PENDING"
                    val isActive = status == "ACTIVE" || status == "IN_PROGRESS"

                    val minutesToDeparture = calculateMinutesToDeparture()
                    callback(isActive, minutesToDeparture)
                } else {
                    callback(false, Long.MAX_VALUE)
                }
            }
            .addOnFailureListener {
                callback(false, Long.MAX_VALUE)
            }
    }

    private fun checkActiveTrip(onResult: (String?) -> Unit) {
        val db = FirebaseFirestore.getInstance()

        // Buscar cualquier viaje activo del conductor actual
        db.collection("Trips")
            .whereEqualTo("idDriver", getCurrentDriverId()) // Necesitas implementar este método
            .whereIn("status", listOf("ACTIVE", "IN_PROGRESS"))
            .get()
            .addOnSuccessListener { querySnapshot ->
                if (!querySnapshot.isEmpty) {
                    val activeTrip = querySnapshot.documents[0]
                    onResult(activeTrip.id)
                } else {
                    onResult(null)
                }
            }
            .addOnFailureListener {
                onResult(null)
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
        checkActiveTrip { activeTripId ->
            if (activeTripId != null && activeTripId != trip.tripId) {
                // Ya hay otro viaje activo
                AlertDialog.Builder(requireContext())
                    .setTitle("Viaje activo")
                    .setMessage("Ya tienes un viaje en curso. ¿Quieres finalizarlo e iniciar este nuevo viaje?")
                    .setPositiveButton("Sí, cambiar viaje") { _, _ ->
                        // Finalizar el viaje anterior y comenzar el nuevo
                        finalizeTripInFirebase(activeTripId) {
                            startTripInFirebase()
                        }
                    }
                    .setNegativeButton("Cancelar", null)
                    .show()
            } else {
                // No hay viaje activo o es el mismo viaje
                startTripInFirebase()
            }
        }
    }

    private fun startTripInFirebase() {
        val db = FirebaseFirestore.getInstance()

        // Log para debuggear
        android.util.Log.d("TRIP_STATUS", "Iniciando viaje: ${trip.tripId}")
        android.util.Log.d("TRIP_STATUS", "Estado anterior: PENDING -> Nuevo estado: ACTIVE")

        // Actualizar el estado del viaje en Firebase
        val updates = hashMapOf<String, Any>(
            "status" to "ACTIVE",
            "actualStartTime" to com.google.firebase.Timestamp.now(),
            "lastUpdated" to com.google.firebase.Timestamp.now()
        )

        db.collection("Trips").document(trip.tripId)
            .update(updates)
            .addOnSuccessListener {
                android.util.Log.d("TRIP_STATUS", "Viaje actualizado exitosamente a ACTIVE")

                // Solo guardar mínimos datos necesarios en SharedPreferences para navegación
                val sharedPreferences = requireContext().getSharedPreferences("route_data", Context.MODE_PRIVATE)
                val editor = sharedPreferences.edit()
                editor.putString("ACTIVE_TRIP_ID", trip.tripId)
                editor.apply()

                showSuccessDialog()
            }
            .addOnFailureListener { exception ->
                android.util.Log.e("TRIP_STATUS", "Error al actualizar estado del viaje", exception)
                Toast.makeText(requireContext(), "Error al iniciar el viaje: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun finalizeTripInFirebase(tripId: String, onComplete: () -> Unit) {
        val db = FirebaseFirestore.getInstance()

        val updates = hashMapOf<String, Any>(
            "status" to "TERMINATED",
            "endTime" to com.google.firebase.Timestamp.now(),
            "lastUpdated" to com.google.firebase.Timestamp.now()
        )

        db.collection("Trips").document(tripId)
            .update(updates)
            .addOnSuccessListener {
                onComplete()
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Error al finalizar viaje anterior", Toast.LENGTH_SHORT).show()
                onComplete() // Continuar de todas formas
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
        checkTripStatusFromFirebase { isActive, _ ->
            if (isActive) {
                Toast.makeText(
                    requireContext(),
                    "No puedes editar un viaje que está en curso",
                    Toast.LENGTH_SHORT
                ).show()
                return@checkTripStatusFromFirebase
            }

            // Continuar con la lógica de edición...
            val intent = Intent(requireContext(), PublishTripFlowActivity::class.java)
            intent.putExtra("IS_EDITING", true)
            intent.putExtra("TRIP_ID", trip.tripId)
            startActivity(intent)
            dismiss()
        }
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
        val db = FirebaseFirestore.getInstance()

        val updates = hashMapOf<String, Any>(
            "status" to "CANCELLED",
            "cancelledAt" to com.google.firebase.Timestamp.now(),
            "lastUpdated" to com.google.firebase.Timestamp.now()
        )

        db.collection("Trips").document(trip.tripId)
            .update(updates)
            .addOnSuccessListener {
                // Limpiar SharedPreferences solo si era el viaje activo
                val sharedPreferences = requireContext().getSharedPreferences("route_data", Context.MODE_PRIVATE)
                val activeTripId = sharedPreferences.getString("ACTIVE_TRIP_ID", "")

                if (activeTripId == trip.tripId) {
                    val editor = sharedPreferences.edit()
                    editor.remove("ACTIVE_TRIP_ID")
                    editor.apply()
                }

                Toast.makeText(requireContext(), "Viaje cancelado exitosamente", Toast.LENGTH_SHORT).show()
                onTripUpdated?.invoke()
                dismiss()
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Error al cancelar el viaje", Toast.LENGTH_SHORT).show()
            }
    }

    private fun getCurrentDriverId(): String {
        // Ejemplo con Firebase Auth:
        return FirebaseAuth.getInstance().currentUser?.uid ?: ""
    }
}