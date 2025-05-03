package com.example.uniride.ui.passenger.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.uniride.databinding.FragmentPassengerHomeBinding
import com.google.android.material.bottomsheet.BottomSheetBehavior

class PassengerHomeFragment : Fragment() {

    private var _binding: FragmentPassengerHomeBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPassengerHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val behavior = BottomSheetBehavior.from(binding.bottomSheet)

        binding.bottomSheet.post {
            behavior.peekHeight = 600
            behavior.state = BottomSheetBehavior.STATE_COLLAPSED
            behavior.isDraggable = true
            behavior.isHideable = false
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
