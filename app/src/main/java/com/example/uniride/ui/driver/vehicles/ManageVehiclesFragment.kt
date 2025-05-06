package com.example.uniride.ui.driver.vehicles

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.uniride.R
import com.example.uniride.databinding.FragmentManageVehiclesBinding
import com.example.uniride.domain.adapter.VehicleAdapter
import com.example.uniride.domain.model.Vehicle

class ManageVehiclesFragment : Fragment() {

    private var _binding: FragmentManageVehiclesBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: VehicleAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentManageVehiclesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupListeners()
        loadVehicles()
    }

    private fun setupRecyclerView() {
        adapter = VehicleAdapter()
        binding.rvVehicles.layoutManager = LinearLayoutManager(requireContext()) // FALTABA
        binding.rvVehicles.adapter = adapter
    }

    private fun loadVehicles() {
        val vehicles = listOf(
            Vehicle("Toyota", "Corolla", 2020, "Blanco", "ABC123"),
            Vehicle("Mazda", "3", 2022, "Gris", "XYZ987")
        )
        adapter.submitList(vehicles)
    }

    private fun setupListeners() {
        binding.btnBack.setOnClickListener {
            requireActivity().finish()
        }


        binding.btnAddVehicle.setOnClickListener {
            findNavController().navigate(R.id.action_manageVehiclesFragment_to_addVehicleFragment3)
        }
    }




    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
