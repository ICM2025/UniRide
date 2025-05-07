package com.example.uniride.ui.passenger.profile

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.uniride.R
import com.example.uniride.connection.SupabaseInstance
import com.example.uniride.databinding.FragmentPassengerProfileBinding
import io.github.jan.supabase.auth.auth
import kotlinx.serialization.json.*


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
        //Se carga el usuario
        val user = SupabaseInstance.client.auth.currentUserOrNull()
        //Se imprime el nombre y el email del usuario
        user?.let {
            val email = it.email
            val username = it.userMetadata?.get("username")?.jsonPrimitive?.content
            binding.tvName.text = username
            binding.tvEmail.text = email
        }

    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}