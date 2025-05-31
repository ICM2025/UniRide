package com.example.uniride.ui.passenger.drawer

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import com.example.uniride.R
import com.example.uniride.databinding.ActivityPassengerDrawerFlowBinding

class PassengerDrawerFlowActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPassengerDrawerFlowBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPassengerDrawerFlowBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 1. Obtener el navHost y navController
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.passenger_drawer_nav_host) as NavHostFragment
        val navController = navHostFragment.navController

        // 2. Cargar el grafo sin modificar el startDestination
        navController.setGraph(R.navigation.passenger_drawer_nav_graph, null)

        // 3. Si se recibió un destino desde el intent, navegar manualmente
        val destinationId = intent.getIntExtra("destination", -1)
        if (destinationId != -1 && destinationId != navController.graph.startDestinationId) {
            navController.navigate(destinationId)
        }
    }
}


