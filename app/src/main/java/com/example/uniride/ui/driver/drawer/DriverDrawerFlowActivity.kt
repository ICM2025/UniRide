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

        //configura el navhost
        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.driver_drawer_nav_host) as NavHostFragment
        val navController = navHostFragment.navController

        navController.setGraph(R.navigation.driver_drawer_nav_graph, null)

        //se usa el perfil por defecto
        val destinationId = intent.getIntExtra("destination", R.id.driverProfileFragment)
        if (destinationId != navController.graph.startDestinationId) {
            navController.navigate(destinationId)
        }
    }
}
