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

        //configurar navhost
        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.passenger_drawer_nav_host) as NavHostFragment
        val navController = navHostFragment.navController

        // cargar grafo
        navController.setGraph(R.navigation.passenger_drawer_nav_graph, null)

        // navegar al destino indicado por parámetro
        val destinationId = intent.getIntExtra("destination", R.id.passengerProfileFragment)
        if (destinationId != navController.graph.startDestinationId) {
            navController.navigate(destinationId)
        }
    }
}


