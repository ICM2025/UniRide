package com.example.uniride.ui

import android.os.Bundle
import android.view.Menu
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import com.example.uniride.R
import com.example.uniride.databinding.ActivityMainBinding
import com.example.uniride.domain.model.DrawerOption
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController
    private val viewModel: MainViewModel by viewModels()


    private val passengerOptions = listOf(
        DrawerOption.Profile,
        DrawerOption.Settings,
        DrawerOption.Logout
    )

    private val driverOptions = listOf(
        DrawerOption.Profile,
        DrawerOption.Settings,
        DrawerOption.ManageVehicles,
        DrawerOption.Statistics,
        DrawerOption.Logout
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navHostFragment = supportFragmentManager.findFragmentById(R.id.main_nav_host) as NavHostFragment
        navController = navHostFragment.navController

        binding.btnMenu.setOnClickListener {
            binding.drawerLayout.openDrawer(GravityCompat.START)
        }

        // acá van los listeners del drawer
        binding.navigationView.setNavigationItemSelectedListener { item ->
            when (item.itemId) {
                DrawerOption.Profile.id -> { /* navController.navigate(...) */ }
                DrawerOption.Settings.id -> { /* navController.navigate(...) */ }
                DrawerOption.ManageVehicles.id -> { /* navController.navigate(...) */ }
                DrawerOption.Statistics.id -> { /* navController.navigate(...) */ }
                DrawerOption.Logout.id -> { /* viewModel.logout() */ }
            }
            binding.drawerLayout.closeDrawer(GravityCompat.START)
            true
        }

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

        // Inflar menú inferior
        binding.bottomNav.menu.clear()
        binding.bottomNav.inflateMenu(
            if (isPassengerMode) R.menu.bottom_nav_menu_passenger
            else R.menu.bottom_nav_menu_driver
        )



        // Seleccionar fragmento inicial
        val startDest = if (isPassengerMode) R.id.passengerHomeFragment else R.id.driverHomeFragment
        navController.navigate(startDest)

        binding.bottomNav.selectedItemId = startDest

        binding.bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.passengerHomeFragment,
                R.id.passengerRequestsFragment,
                R.id.driverHomeFragment,
                R.id.driverTripsFragment -> {
                    navController.navigate(item.itemId)
                    true
                }
                R.id.switchToDriver -> {
                    viewModel.switchToDriver()
                    true
                }
                R.id.switchToPassenger -> {
                    viewModel.switchToPassenger()
                    true
                }
                else -> false
            }
        }

        // Cargar  el menú lateral según si es conductor o pasajero
        val options = if (isPassengerMode) passengerOptions else driverOptions
        loadDrawerMenu(options)
    }

    //cargar el menú lateral
    private fun loadDrawerMenu(options: List<DrawerOption>) {
        val menu = binding.navigationView.menu
        menu.clear()
        options.forEach { option ->
            menu.add(Menu.NONE, option.id, Menu.NONE, option.title)
                .setIcon(option.iconRes)
        }
    }
}
