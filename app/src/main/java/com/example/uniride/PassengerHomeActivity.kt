package com.example.uniride

import android.content.Intent
import android.os.Bundle
import android.view.View
import com.example.uniride.databinding.ActivityPassengerHomeBinding
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.squareup.picasso.Picasso

class PassengerHomeActivity : BottomMenuActivity() {
    private lateinit var binding: ActivityPassengerHomeBinding
    private lateinit var bottomSheetBehavior: BottomSheetBehavior<View>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPassengerHomeBinding.inflate(layoutInflater)

        binding = ActivityPassengerHomeBinding.inflate(layoutInflater)
        bottomMenuBinding.container.removeAllViews()
        bottomMenuBinding.container.addView(binding.root)

        // Usar la vista raíz del binding para el contenedor del menú inferior
        bottomMenuBinding.container.removeAllViews()
        bottomMenuBinding.container.addView(binding.root)
        bottomMenuBinding.bottomNav.selectedItemId = R.id.nav_home

        setupBottomSheet()
        loadImage()
        binding.cardSearch.setOnClickListener { navigateToSearchWheelActivity() }

        binding.buscarConductor.setOnClickListener {
            val intent = Intent(this, DriverAviableActivity::class.java)
            startActivity(intent)

        }
    }

    private fun navigateToSearchWheelActivity() {
        val intent = Intent(baseContext, SearchWheelActivity::class.java)
        startActivity(intent)
    }

    private fun loadImage() {
        val imageUrl =
            "https://media.wired.com/photos/59269cd37034dc5f91bec0f1/master/w_1600,c_limit/GoogleMapTA.jpg"

        Picasso.get()
            .load(imageUrl)
            .placeholder(R.drawable.ic_launcher_background)
            .error(R.drawable.ic_launcher_background)
            .into(binding.imageViewMap)

    }

    private fun setupBottomSheet() {
        // Inicializa el BottomSheet
        bottomSheetBehavior = BottomSheetBehavior.from(binding.bottomSheet)

        // Comienza colapsado
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED


        // Configurar listeners para los destinos (por ahora no)
        binding.layoutDestination1.setOnClickListener {

        }

        binding.layoutDestination2.setOnClickListener {

        }

        binding.cardSearch.setOnClickListener {
            // Expandir el bottom sheet cuando se pulsa la barra de búsqueda
            //No se nota por el intent
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
        }
    }


}