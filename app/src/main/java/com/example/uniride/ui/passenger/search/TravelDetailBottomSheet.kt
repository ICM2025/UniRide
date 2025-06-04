package com.example.uniride.ui.passenger.search

import android.app.AlertDialog
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import com.example.uniride.R
import com.example.uniride.databinding.BottomSheetTravelDetailBinding
import com.example.uniride.domain.model.PassengerRequest
import com.example.uniride.domain.model.RequestStatus
import com.example.uniride.domain.model.TravelOption
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class TravelDetailBottomSheet(private val travel: TravelOption, private val tripId: String) : BottomSheetDialogFragment() {

    private var _binding: BottomSheetTravelDetailBinding? = null
    private val binding get() = _binding!!

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetTravelDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.ivIcon.setImageResource(travel.driverImage)
        binding.ivCar.setImageResource(travel.drawableResId)
        binding.tvDriverName.text = "Conductor: ${travel.driverName}"
        binding.tvRoute.text = "Ruta: ${travel.origin} → ${travel.destination}"
        binding.tvStops.text = "Paradas: ${travel.intermediateStops.joinToString(", ")}"
        binding.tvTime.text = "Hora de salida: ${travel.departureTime}"
        binding.tvPrice.text = "Precio: $${travel.price}"
        binding.tvSeats.text = "Cupos disponibles: ${travel.availableSeats}"

        binding.btnRequest.setOnClickListener {
            savePassengerRequest()
        }
    }

    private fun savePassengerRequest() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Toast.makeText(requireContext(), "Debes iniciar sesión", Toast.LENGTH_SHORT).show()
            return
        }

        // Crear el objeto PassengerRequest
        val passengerRequest = PassengerRequest(
            idTrip = tripId,
            idUser = currentUser.uid,
            status = RequestStatus.PENDING
        )

        // Guardar en Firestore
        db.collection("PassengerRequests")
            .add(passengerRequest)
            .addOnSuccessListener { documentReference ->
                Log.d("PassengerRequest", "Solicitud guardada con ID: ${documentReference.id}")
                showSuccessDialog()
            }
            .addOnFailureListener { e ->
                Log.e("PassengerRequest", "Error al guardar solicitud", e)
                Toast.makeText(requireContext(), "Error al enviar solicitud", Toast.LENGTH_SHORT).show()
            }
    }

    private fun showSuccessDialog() {
        val activity = requireActivity()
        dismiss()

        val dialogView = layoutInflater.inflate(R.layout.dialog_success_request, null)

        dialogView.findViewById<TextView>(R.id.tv_success).text = "¡Cupo solicitado!"
        dialogView.findViewById<TextView>(R.id.tv_secondary).text = "El conductor ha sido notificado"

        val dialog = AlertDialog.Builder(activity)
            .setView(dialogView)
            .create()

        dialog.window?.setDimAmount(0.75f)
        dialog.window?.attributes?.windowAnimations = R.style.DialogFadeAnimation

        dialog.show()

        Handler(Looper.getMainLooper()).postDelayed({
            dialog.dismiss()
            activity.finish()
        }, 1500)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
