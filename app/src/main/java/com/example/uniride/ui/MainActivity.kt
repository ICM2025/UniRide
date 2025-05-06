package com.example.uniride.ui

import android.content.Intent
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
import com.example.uniride.ui.driver.drawer.DriverDrawerFlowActivity
import com.example.uniride.ui.passenger.drawer.PassengerDrawerFlowActivity
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController
    private val viewModel: MainViewModel by viewModels()
    //para los menus laterales
    private val passengerOptions = listOf(
        DrawerOption.Profile,
        DrawerOption.Settings,
        DrawerOption.About,
        DrawerOption.Logout
    )
    private val driverOptions = listOf(
        DrawerOption.Profile,
        DrawerOption.Settings,
        DrawerOption.About,
        DrawerOption.ManageVehicles,
        DrawerOption.Statistics,
        DrawerOption.Logout
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navHostFragment = supportFragmentManager.findFragmentById(R.id.main_nav_host)
                as NavHostFragment
        navController = navHostFragment.navController

        binding.btnMenu.setOnClickListener {
            binding.drawerLayout.openDrawer(GravityCompat.START)
        }

        setupDrawerItemClicks()

        lifecycleScope.launch {
            viewModel.isPassengerMode.collectLatest { isPassengerMode ->
                setupNavigation(isPassengerMode)
            }
        }
    }

    private fun setupDrawerItemClicks() {
        // acá van los listeners del drawer
        binding.navigationView.setNavigationItemSelectedListener { item ->
            val isPassenger = viewModel.isPassengerMode.value

            val (activityClass, destination) = when (item.itemId) {
                DrawerOption.Profile.id -> {
                    if (isPassenger)
                        PassengerDrawerFlowActivity::class.java to R.id.passengerProfileFragment
                    else
                        DriverDrawerFlowActivity::class.java to R.id.driverProfileFragment
                }

                DrawerOption.Settings.id -> {
                    if (isPassenger)
                        PassengerDrawerFlowActivity::class.java to R.id.settingsFragment
                    else
                        DriverDrawerFlowActivity::class.java to R.id.settingsFragment2
                }

                DrawerOption.ManageVehicles.id -> {
                    DriverDrawerFlowActivity::class.java to R.id.manageVehiclesFragment
                }

                DrawerOption.Statistics.id -> {
                    DriverDrawerFlowActivity::class.java to R.id.statisticsFragment
                }

                DrawerOption.About.id -> {
                    if (isPassenger)
                        PassengerDrawerFlowActivity::class.java to R.id.aboutFragment
                    else
                        DriverDrawerFlowActivity::class.java to R.id.aboutFragment2
                }

                DrawerOption.Logout.id -> {
                    //ACÁ HACER EL LOGOUT
                    return@setNavigationItemSelectedListener true
                }

                else -> return@setNavigationItemSelectedListener false
            }

            val intent = Intent(this, activityClass)
            intent.putExtra("destination", destination)
            startActivity(intent)
            //cierra menu lateral
            binding.drawerLayout.closeDrawer(GravityCompat.START)
            true
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
                R.id.driverTripsFragment,
                R.id.tripRequestsFragment -> {
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
