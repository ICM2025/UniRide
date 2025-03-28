package com.example.uniride

import android.os.Bundle
import android.widget.ArrayAdapter
import com.example.uniride.model.Solicitud
import com.example.uniride.databinding.ActivitySearchWheelBinding
import com.example.uniride.adapters.SolicitudAdapter

class SearchWheelActivity : BottomMenuActivity() {
    private lateinit var binding: ActivitySearchWheelBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySearchWheelBinding.inflate(layoutInflater)

        bottomMenuBinding.container.removeAllViews()
        bottomMenuBinding.container.addView(binding.root)

        // Datos temporales
        val solicitudes = listOf(
            Solicitud("Titan Plaza", "Javeriana, 5 cupos", "$5000"),
            Solicitud("Chía", "Javeriana, 5 cupos", "$6000"),
            Solicitud("Colina", "Javeriana, 5 cupos", "$5000"),
            Solicitud("Modelia", "Javeriana, 5 cupos", "$4000"),
            Solicitud("Suba", "Javeriana, 5 cupos", "$5000"),
            Solicitud("Kennedy", "Javeriana, 5 cupos", "$5000"),
            Solicitud("Jumbo", "Javeriana, 5 cupos", "$5000"),
            Solicitud("Calle 80", "Javeriana, 5 cupos", "$4000")
        )

        val adapter = SolicitudAdapter(this, solicitudes)
        binding.requestsListView.adapter = adapter

        // Select the home item in the bottom nav
        bottomMenuBinding.bottomNav.selectedItemId = R.id.nav_home
    }
}

