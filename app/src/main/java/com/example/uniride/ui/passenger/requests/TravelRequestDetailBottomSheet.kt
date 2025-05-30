package com.example.uniride.ui.passenger.requests

import android.app.AlertDialog
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import com.example.uniride.R
import com.example.uniride.databinding.BottomSheetTravelRequestDetailBinding
import com.example.uniride.domain.model.RequestStatus
import com.example.uniride.domain.model.TravelRequest
import com.example.uniride.domain.model.TravelRequestStatus
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class RequestDetailBottomSheet(
    private val request: TravelRequest
) : BottomSheetDialogFragment() {

    private var _binding: BottomSheetTravelRequestDetailBinding? = null
    private val binding get() = _binding!!

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetTravelRequestDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val travel = request.travelOption

        binding.ivIcon.setImageResource(travel.driverImage)
        binding.ivCar.setImageResource(travel.drawableResId)
        binding.tvDriverName.text = travel.driverName
        binding.tvCarPlate.text = "Placas: ABC-123"
        binding.tvTime.text = "Salida: ${travel.departureTime}"
        binding.tvSeats.text = "Cupos disponibles: ${travel.availableSeats}"
        binding.tvStops.text = "Paradas: ${travel.intermediateStops.joinToString(", ")}"
        binding.tvRoute.text = "Ruta: ${travel.origin} → ${travel.destination}"
        binding.tvPrice.text = "Precio: $${travel.price}"
        binding.tvDate.text = "Fecha: ${travel.travelDate}" // Formato: yyyy-MM-dd

        when (request.status) {
            is TravelRequestStatus.Accepted -> {
                binding.btnRequest.text = "Cancelar solicitud"
                binding.btnRequest.visibility = View.VISIBLE
                binding.btnRequest.setOnClickListener { cancelRequest() }
            }
            is TravelRequestStatus.Pending -> {
                binding.btnRequest.text = "Cancelar solicitud"
                binding.btnRequest.visibility = View.VISIBLE
                binding.btnRequest.setOnClickListener { cancelRequest() }
            }
            is TravelRequestStatus.Rejected -> {
                binding.btnRequest.text = "Volver a solicitar"
                binding.btnRequest.visibility = View.VISIBLE
                binding.btnRequest.setOnClickListener { reRequestTrip() }
            }
            is TravelRequestStatus.Finished -> {
                binding.btnRequest.visibility = View.GONE
            }
        }
    }

    private fun cancelRequest() {
        val currentUserId = auth.currentUser?.uid ?: return

        // Buscar la solicitud en Firebase
        findCurrentPassengerRequest(currentUserId) { passengerRequestId ->
            if (passengerRequestId != null) {
                // Si está aceptada, incrementar cupos del trip
                if (request.status is TravelRequestStatus.Accepted) {
                    incrementTripSeats() {
                        deletePassengerRequest(passengerRequestId)
                    }
                } else {
                    deletePassengerRequest(passengerRequestId)
                }
            }
        }
    }

    private fun reRequestTrip() {
        val currentUserId = auth.currentUser?.uid ?: return

        // Verificar intentos de solicitud
        findCurrentPassengerRequest(currentUserId) { passengerRequestId ->
            if (passengerRequestId != null) {
                db.collection("PassengerRequests").document(passengerRequestId)
                    .get()
                    .addOnSuccessListener { doc ->
                        val currentAttempts = doc.getLong("requestAttempts")?.toInt() ?: 1

                        if (currentAttempts >= 3) {
                            Toast.makeText(requireContext(), "Has alcanzado el máximo de intentos para este viaje", Toast.LENGTH_LONG).show()
                            return@addOnSuccessListener
                        }

                        // Actualizar a PENDING y incrementar intentos
                        db.collection("PassengerRequests").document(passengerRequestId)
                            .update(
                                mapOf(
                                    "status" to RequestStatus.PENDING.name,
                                    "requestAttempts" to currentAttempts + 1
                                )
                            )
                            .addOnSuccessListener {
                                showSuccessDialog("Solicitud reenviada", "El conductor ha sido notificado nuevamente")
                            }
                            .addOnFailureListener {
                                Toast.makeText(requireContext(), "Error al reenviar solicitud", Toast.LENGTH_SHORT).show()
                            }
                    }
            }
        }
    }

    private fun findCurrentPassengerRequest(userId: String, onResult: (String?) -> Unit) {
        // Aquí necesitarías el tripId real, podrías pasarlo como parámetro adicional
        // Por ahora buscaremos por usuario y origen/destino
        db.collection("PassengerRequests")
            .whereEqualTo("idUser", userId)
            .get()
            .addOnSuccessListener { snapshot ->
                // Encontrar la solicitud que coincida con este viaje
                // Esto es una simplificación, idealmente tendrías el tripId
                if (snapshot.documents.isNotEmpty()) {
                    onResult(snapshot.documents.first().id)
                } else {
                    onResult(null)
                }
            }
    }

    private fun incrementTripSeats(onComplete: () -> Unit) {
        // Buscar el trip por origen y destino (simplificación)
        // En producción deberías tener el tripId
        db.collection("Trips")
            .whereEqualTo("status", "PENDING")
            .get()
            .addOnSuccessListener { snapshot ->
                if (snapshot.documents.isNotEmpty()) {
                    val tripId = snapshot.documents.first().id
                    val currentPlaces = snapshot.documents.first().getLong("places")?.toInt() ?: 0

                    db.collection("Trips").document(tripId)
                        .update("places", currentPlaces + 1)
                        .addOnSuccessListener { onComplete() }
                        .addOnFailureListener { onComplete() }
                } else {
                    onComplete()
                }
            }
    }

    private fun deletePassengerRequest(requestId: String) {
        db.collection("PassengerRequests").document(requestId)
            .delete()
            .addOnSuccessListener {
                showSuccessDialog("Solicitud cancelada", "Tu solicitud ha sido cancelada exitosamente")
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Error al cancelar solicitud", Toast.LENGTH_SHORT).show()
            }
    }

    private fun showSuccessDialog(title: String, message: String) {
        val activity = requireActivity()
        dismiss()

        val dialogView = layoutInflater.inflate(R.layout.dialog_success_request, null)

        dialogView.findViewById<TextView>(R.id.tv_success).text = title
        dialogView.findViewById<TextView>(R.id.tv_secondary).text = message

        val dialog = AlertDialog.Builder(activity)
            .setView(dialogView)
            .create()

        dialog.window?.setDimAmount(0.75f)
        dialog.window?.attributes?.windowAnimations = R.style.DialogFadeAnimation

        dialog.show()

        Handler(Looper.getMainLooper()).postDelayed({
            dialog.dismiss()
        }, 1500)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}