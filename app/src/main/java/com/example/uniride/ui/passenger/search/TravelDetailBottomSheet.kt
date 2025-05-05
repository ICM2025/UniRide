package com.example.uniride.ui.passenger.search

import android.app.AlertDialog
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.uniride.R
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


        //para solicitar cupo
        binding.btnRequest.setOnClickListener {
            //referencia a la actividad actual
            val activity = requireActivity()
            //cierra el sheet dialog fragment (el de detalle de viaje)
            dismiss()

            //infla para mensaje de confirmación
            val dialogView = layoutInflater.inflate(R.layout.dialog_success_request, null)

            //crea AlertDialog usando el layout inflado
            val dialog = AlertDialog.Builder(activity)
                .setView(dialogView)
                .create()
            //simular desenfoque en el resto de la pantalla
            dialog.window?.setDimAmount(0.75f)
            // animación de entrada para popup
            dialog.window?.attributes?.windowAnimations = R.style.DialogFadeAnimation

            dialog.show()

            Handler(Looper.getMainLooper()).postDelayed({
                //cierra dialogo
                dialog.dismiss()
                //cerrar actividad actual para devolverse al home
                activity.finish()
            }, 1500)

        }

    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
