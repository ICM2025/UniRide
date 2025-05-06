package com.example.uniride.ui.passenger.search

import android.Manifest
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.uniride.R
import com.example.uniride.databinding.FragmentSearchInputBinding
import com.example.uniride.domain.adapter.PlaceAdapter
import com.example.uniride.domain.model.Place
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import java.io.IOException
import java.util.Locale

class SearchInputFragment : Fragment() {

    private var _binding: FragmentSearchInputBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: PlaceAdapter
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var activeField: EditTextField = EditTextField.DESTINATION

    enum class EditTextField {
        ORIGIN, DESTINATION
    }

    // Lista de lugares que se mostrará en el RecyclerView
    private val placesList = listOf(
        Place("Mi ubicación actual", "Usa tu ubicación GPS"),
        Place("Universidad Javeriana", "Cra. 7 #45, Bogotá"),
        Place("Centro Internacional", "Av. 19 #32, Bogotá"),
        Place("Terminal Salitre", "Calle 22 #68, Bogotá")
    )

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSearchInputBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())

        adapter = PlaceAdapter(placesList) { selected ->
            if (selected.name == "Mi ubicación actual") {
                getCurrentLocation()
            } else {
                fillSelectedPlace(selected)
            }
        }

        binding.rvRecentPlaces.layoutManager = LinearLayoutManager(requireContext())
        binding.rvRecentPlaces.adapter = adapter

        // Forzar apertura de teclado
        binding.etDestination.requestFocus()
        requireActivity().window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE)

        initListeners()
        setupTextWatchers()
    }

    private fun fillSelectedPlace(place: Place) {
        when (activeField) {
            EditTextField.ORIGIN -> binding.etOrigin.setText(place.name)
            EditTextField.DESTINATION -> binding.etDestination.setText(place.name)
        }
        updateSearchButtonVisibility()
    }

    private fun getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ),
                LOCATION_PERMISSION_REQUEST_CODE
            )
            return
        }

        fusedLocationClient.lastLocation
            .addOnSuccessListener { location ->
                if (location != null) {
                    try {
                        val geocoder = Geocoder(requireContext(), Locale.getDefault())
                        val addresses: List<Address>? = geocoder.getFromLocation(
                            location.latitude,
                            location.longitude,
                            1
                        )

                        if (!addresses.isNullOrEmpty()) {
                            val address = addresses[0]
                            val addressText = address.getAddressLine(0) ?: "Mi ubicación actual"

                            when (activeField) {
                                EditTextField.ORIGIN -> binding.etOrigin.setText(addressText)
                                EditTextField.DESTINATION -> binding.etDestination.setText(addressText)
                            }
                            updateSearchButtonVisibility()
                        }
                    } catch (e: IOException) {
                        Toast.makeText(
                            requireContext(),
                            "No se pudo obtener la dirección",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } else {
                    Toast.makeText(
                        requireContext(),
                        "No se pudo obtener la ubicación",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
            .addOnFailureListener {
                Toast.makeText(
                    requireContext(),
                    "Error al obtener la ubicación",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    private fun initListeners() {
        binding.btnBack.setOnClickListener {
            requireActivity().finish()
        }

        binding.btnSearch.setOnClickListener {
            val origin = binding.etOrigin.text.toString().trim()
            val destination = binding.etDestination.text.toString().trim()

            val bundle = Bundle().apply {
                putString("origin", origin)
                putString("destination", destination)
            }

            findNavController().navigate(
                R.id.action_searchInputFragment_to_searchResultsFragment,
                bundle
            )
        }

        // Set focus listeners to track which field is active
        binding.etOrigin.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                activeField = EditTextField.ORIGIN
            }
        }

        binding.etDestination.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                activeField = EditTextField.DESTINATION
            }
        }
    }

    //para verificar si los cuadros te texto están vacíos. necesario para saber si mostrar o no botón
    private fun setupTextWatchers() {
        val watcher = object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                updateSearchButtonVisibility()
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        }

        binding.etOrigin.addTextChangedListener(watcher)
        binding.etDestination.addTextChangedListener(watcher)
    }

    private fun updateSearchButtonVisibility() {
        val origin = binding.etOrigin.text.toString().trim()
        val destination = binding.etDestination.text.toString().trim()
        binding.btnSearch.visibility = if (origin.isNotEmpty() && destination.isNotEmpty()) {
            View.VISIBLE
        } else {
            View.GONE
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getCurrentLocation()
            } else {
                Toast.makeText(
                    requireContext(),
                    "Permiso de ubicación denegado",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 100
    }
}