package com.example.uniride.ui.passenger.search

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.uniride.R
import com.example.uniride.databinding.FragmentSearchInputBinding
import com.example.uniride.domain.adapter.PlaceAdapter
import com.example.uniride.domain.model.Place

class SearchInputFragment : Fragment() {

    private var _binding: FragmentSearchInputBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: PlaceAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSearchInputBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val provitionalPlaces = listOf(
            Place("Universidad Nacional", "Cra. 30 #45, Bogotá"),
            Place("Centro Internacional", "Av. 19 #32, Bogotá"),
            Place("Terminal Salitre", "Calle 22 #68, Bogotá")
        )

        adapter = PlaceAdapter(provitionalPlaces) { selected ->
            Toast.makeText(requireContext(), "Seleccionaste: ${selected.name}", Toast.LENGTH_SHORT)
                .show()
        }

        binding.rvRecentPlaces.layoutManager = LinearLayoutManager(requireContext())
        binding.rvRecentPlaces.adapter = adapter

        // Forzar apertura de teclado
        binding.etDestination.requestFocus()
        requireActivity().window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE)


        initListeners()
        setupTextWatchers()
    }

    private fun initListeners() {
        binding.btnBack.setOnClickListener {
            requireActivity().finish()
        }

        binding.btnSearch.setOnClickListener {
            findNavController().navigate(R.id.action_searchInputFragment_to_searchResultsFragment)
        }
    }

    //para verificar si los cuadros te texto están vacíos. necesario para saber si mostrar o no botón
    private fun setupTextWatchers() {
        val watcher = object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val origin = binding.etOrigin.text.toString().trim()
                val destination = binding.etDestination.text.toString().trim()
                binding.btnSearch.visibility = if (origin.isNotEmpty() && destination.isNotEmpty()) {
                    View.VISIBLE
                } else {
                    View.GONE
                }
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        }

        binding.etOrigin.addTextChangedListener(watcher)
        binding.etDestination.addTextChangedListener(watcher)
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }


}
