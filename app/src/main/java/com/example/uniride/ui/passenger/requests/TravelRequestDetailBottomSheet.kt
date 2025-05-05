package com.example.uniride.ui.passenger.requests

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.uniride.databinding.BottomSheetTravelRequestDetailBinding
import com.example.uniride.domain.model.TravelRequest
import com.example.uniride.domain.model.TravelRequestStatus
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class RequestDetailBottomSheet(
    private val request: TravelRequest
) : BottomSheetDialogFragment() {

    private var _binding: BottomSheetTravelRequestDetailBinding? = null
    private val binding get() = _binding!!

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
            is TravelRequestStatus.Accepted,
            is TravelRequestStatus.Pending -> {
                binding.btnRequest.text = "Cancelar solicitud"
                binding.btnRequest.visibility = View.VISIBLE
                // Aquí puedes añadir lógica de cancelación
            }
            is TravelRequestStatus.Rejected -> {
                binding.btnRequest.text = "Volver a solicitar"
                binding.btnRequest.visibility = View.VISIBLE
                // Lógica para reintentar solicitud
            }
            is TravelRequestStatus.Finished -> {
                binding.btnRequest.visibility = View.GONE
            }
        }

        binding.btnRequest.setOnClickListener {

            dismiss()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}