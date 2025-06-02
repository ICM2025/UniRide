package com.example.uniride.ui.driver.drawer

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import com.example.uniride.R
import com.example.uniride.databinding.ActivityDriverDrawerFlowBinding

class DriverDrawerFlowActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDriverDrawerFlowBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDriverDrawerFlowBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navHostFragment = supportFragmentManager.findFragmentById(R.id.driver_drawer_nav_host) as NavHostFragment
        val navController = navHostFragment.navController

        // 1. Cargar grafo normal sin modificar startDestination
        navController.setGraph(R.navigation.driver_drawer_nav_graph, null)

        // 2. Si se indica un destino, navegar manualmente (una vez el grafo está cargado)
        val destinationId = intent.getIntExtra("destination", -1)
        if (destinationId != -1 && destinationId != navController.graph.startDestinationId) {
            navController.navigate(destinationId)
        }
    }

}
