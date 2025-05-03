package com.example.uniride.ui

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import com.example.uniride.R
import com.example.uniride.databinding.ActivityMainBinding
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navHostFragment = supportFragmentManager.findFragmentById(R.id.main_nav_host) as NavHostFragment
        navController = navHostFragment.navController

        // lanza corrutina para observar el modo actual (pasajero o conductor)
        lifecycleScope.launch {
            viewModel.isPassengerMode.collectLatest { isPassengerMode ->
                setupNavigation(isPassengerMode)
            }
        }
    }

    private fun setupNavigation(isPassengerMode: Boolean) {
        val navInflater = navController.navInflater
        val graph = navInflater.inflate(
            if (isPassengerMode) R.navigation.passenger_nav_graph
            else R.navigation.driver_nav_graph
        )
        navController.graph = graph

        // Inflar el menú correspondiente
        binding.bottomNav.menu.clear()
        binding.bottomNav.inflateMenu(
            if (isPassengerMode) R.menu.bottom_nav_menu_passenger
            else R.menu.bottom_nav_menu_driver
        )

        // Seleccionar el fragment inicial según el modo actual
        val startDest = if (isPassengerMode) R.id.passengerHomeFragment else R.id.driverHomeFragment
        navController.navigate(startDest)

        binding.bottomNav.selectedItemId = startDest

        // listener que maneja las opciones al hacer click en el menú inferior
        binding.bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                //si se selecciona alguna de las opciones del menú -> se navega a ese fragment
                R.id.passengerHomeFragment,
                R.id.passengerRequestsFragment,
                R.id.driverHomeFragment,
                R.id.driverTripsFragment -> {
                    navController.navigate(item.itemId)
                    true
                }
                //cambio a menú  de conductor
                R.id.switchToDriver -> {
                    viewModel.switchToDriver()
                    true
                }
                //cambio a menú  de pasajero
                R.id.switchToPassenger -> {
                    viewModel.switchToPassenger()
                    true
                }

                else -> false
            }
        }
    }

}
