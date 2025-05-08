package com.example.uniride.ui.passenger.search

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.os.Bundle
import android.speech.RecognizerIntent
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.uniride.R
import com.example.uniride.databinding.FragmentSearchInputBinding
import com.example.uniride.domain.adapter.PlaceAdapter
import com.example.uniride.domain.model.Place
import com.example.uniride.utility.Config_permission
import com.google.android.gms.maps.GoogleMap
import java.io.IOException
import java.util.Locale

class SearchInputFragment : Fragment() {

    private var _binding: FragmentSearchInputBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: PlaceAdapter
    private lateinit var configPermission: Config_permission
    private var activeField: EditTextField = EditTextField.DESTINATION
    private var currentLocation: Location? = null

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

    // ActivityResultLauncher para la búsqueda por voz
    private val speechRecognizerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            val spokenText = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.get(0) ?: ""
            when (activeField) {
                EditTextField.ORIGIN -> binding.etOrigin.setText(spokenText)
                EditTextField.DESTINATION -> binding.etDestination.setText(spokenText)
            }
            updateSearchButtonVisibility()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSearchInputBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        configPermission = Config_permission(
            fragment = this,
            mapReadyCallback = { googleMap: GoogleMap -> },
            locationUpdateCallback = { location: Location ->
                // Guardar la ubicación actual pero no la mostramos automáticamente
                currentLocation = location
            }
        )
        configPermission.initialize()

        adapter = PlaceAdapter(placesList) { selected ->
            if (selected.name == "Mi ubicación actual") {
                handleMyLocationSelection()
            } else {
                fillSelectedPlace(selected)
            }
        }

        binding.rvRecentPlaces.layoutManager = LinearLayoutManager(requireContext())
        binding.rvRecentPlaces.adapter = adapter

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

    private fun handleMyLocationSelection() {
        // Verifica si ya tenemos la ubicación almacenada
        val location = currentLocation ?: configPermission.getLastKnownLocation()

        if (location != null) {
            useLocationForAddress(location)
        } else {
            // Si no tenemos ubicación, solicitamos acceso y actualizaciones
            configPermission.checkLocationPermissions()
            Toast.makeText(requireContext(),"Obteniendo tu ubicación...",Toast.LENGTH_SHORT).show()

            view?.postDelayed({
                val updatedLocation = configPermission.getLastKnownLocation()
                if (updatedLocation != null) {
                    useLocationForAddress(updatedLocation)
                } else {
                    Toast.makeText(requireContext(),"No se pudo obtener la ubicación. Intenta nuevamente.",Toast.LENGTH_SHORT).show()
                }
            }, 2000)
        }
    }

    private fun useLocationForAddress(location: Location) {
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
            Toast.makeText(requireContext(),"No se pudo obtener la dirección",Toast.LENGTH_SHORT).show()
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

        binding.btnVoiceOrigin.setOnClickListener {
            activeField = EditTextField.ORIGIN
            startVoiceRecognition()
        }

        binding.btnVoiceDestination.setOnClickListener {
            activeField = EditTextField.DESTINATION
            startVoiceRecognition()
        }
    }

    // Inicia la actividad de reconocimiento de voz
    private fun startVoiceRecognition() {
        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(Manifest.permission.RECORD_AUDIO),
                VOICE_PERMISSION_REQUEST_CODE
            )
            return
        }

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Habla ahora...")
        }

        try {
            speechRecognizerLauncher.launch(intent)
        } catch (e: Exception) {
            Toast.makeText(requireContext(),"Tu dispositivo no soporta reconocimiento de voz",Toast.LENGTH_SHORT).show()
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
        when (requestCode) {
            VOICE_PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    startVoiceRecognition()
                } else {
                    Toast.makeText(requireContext(),"Permiso de micrófono denegado",Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        configPermission.onResume()
    }

    override fun onPause() {
        super.onPause()
        configPermission.onPause()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val VOICE_PERMISSION_REQUEST_CODE = 101
    }
}