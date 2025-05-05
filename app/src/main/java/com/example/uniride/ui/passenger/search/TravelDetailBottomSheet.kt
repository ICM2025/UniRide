package com.example.uniride.ui.passenger.search

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.example.uniride.databinding.BottomSheetTravelDetailBinding
import com.example.uniride.domain.model.TravelOption
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class TravelDetailBottomSheet(private val travel: TravelOption) : BottomSheetDialogFragment() {

    private var _binding: BottomSheetTravelDetailBinding? = null
    private val binding get() = _binding!!

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
            Toast.makeText(requireContext(), "Solicitud enviada", Toast.LENGTH_SHORT).show()
            dismiss()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
