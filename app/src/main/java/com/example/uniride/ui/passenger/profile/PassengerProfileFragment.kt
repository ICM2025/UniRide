package com.example.uniride.ui.passenger.profile

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.uniride.R
import com.example.uniride.databinding.FragmentPassengerProfileBinding

class PassengerProfileFragment : Fragment() {

    private var _binding: FragmentPassengerProfileBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPassengerProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnBack.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }

        // ACÁ SE DEBERÍAN CARGAR DATOS DE PERFIL DESDE DB
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}