package com.example.uniride.ui.driver.trips

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.uniride.databinding.BottomSheetPassengerDetailBinding
import com.example.uniride.domain.model.PassengerRequest
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class PassengerRequestDetailBottomSheet(
    private val details: PassengerRequest,
    private val onOpenChat: () -> Unit
) : BottomSheetDialogFragment() {

    private var _binding: BottomSheetPassengerDetailBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetPassengerDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // asignar datos a la vista
        binding.tvPassengerName.text = details.passengerName
        binding.tvPassengerUniversity.text = details.university
        binding.tvPassengerEmail.text = details.email
        binding.tvTripCount.text = details.tripCount.toString()
        binding.tvRating.text = details.rating.toString()
        binding.tvReviews.text = details.reviewsCount.toString()
        binding.ivPassengerPhoto.setImageResource(details.profileImg)

        // Listener botón
        binding.btnOpenChat.setOnClickListener {
            dismiss()
            onOpenChat()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
