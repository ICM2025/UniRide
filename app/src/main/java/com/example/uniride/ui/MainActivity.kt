package com.example.uniride.ui

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import com.example.uniride.R
import com.example.uniride.chats.ChatsActivity
import com.example.uniride.databinding.ActivityMainBinding
import com.example.uniride.domain.model.DrawerOption
import com.example.uniride.ui.auth.AuthActivity
import com.example.uniride.ui.driver.drawer.DriverDrawerFlowActivity
import com.example.uniride.ui.passenger.drawer.PassengerDrawerFlowActivity
import com.example.uniride.ui.passenger.search.SearchFlowActivity
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlin.math.sqrt

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController
    private lateinit var sharedPreferences: SharedPreferences
    //variables del sensor acelerómetro
    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null
    private var lastShakeTime: Long = 0
    private val sensorListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent?) {
            val now = System.currentTimeMillis()
            if (event == null || (now - lastShakeTime) < 10) return

            val x = event.values[0]
            val y = event.values[1]
            val z = event.values[2]

            val acceleration = sqrt(x * x + y * y + z * z) - SensorManager.GRAVITY_EARTH

            if (acceleration > 12) {
                lastShakeTime = now
                //si es pasajero lleva a buscar un viaje si es conductor a el chat
                val isPassenger = viewModel.isPassengerMode.value
                val activityClass = if (isPassenger)
                    SearchFlowActivity::class.java
                else
                    ChatsActivity::class.java

                val destination = if (isPassenger)
                    R.id.searchInputFragment
                else
                    R.id.chatFragment

                Log.e("acele", "Entrando a ${if (isPassenger) "búsqueda de viaje" else "perfil del conductor"}")

                val intent = Intent(this@MainActivity, activityClass).apply {
                    //para que no se generen multiples instancias en el stack
                    flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                    putExtra("destination", destination)
                }
                startActivity(intent)

            }
        }


        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
    }


    private val viewModel: MainViewModel by viewModels()
    //para los menus laterales
    private val passengerOptions = listOf(
        DrawerOption.Profile,
        DrawerOption.Settings,
        DrawerOption.Chats,
        DrawerOption.About,
        DrawerOption.Logout
    )
    private val driverOptions = listOf(
        DrawerOption.Profile,
        DrawerOption.Settings,
        DrawerOption.Chats,
        DrawerOption.About,
        DrawerOption.ManageVehicles,
        DrawerOption.Statistics,
        DrawerOption.Logout
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        Log.d("MainActivity", "Intent extras: ${intent.extras}")

        val navHostFragment = supportFragmentManager.findFragmentById(R.id.main_nav_host)
                as NavHostFragment
        navController = navHostFragment.navController
        //para no mostrar los botones cuando entre a un chat.
        navController.addOnDestinationChangedListener { _, destination, _ ->
            when (destination.id) {
                R.id.chatFragment -> binding.bottomNav.visibility = View.GONE
                else -> binding.bottomNav.visibility = View.VISIBLE
            }
        }

        binding.btnMenu.setOnClickListener {
            binding.drawerLayout.openDrawer(GravityCompat.START)
        }

        setupDrawerItemClicks()

        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        accelerometer?.let {
            sensorManager.registerListener(sensorListener, it, SensorManager.SENSOR_DELAY_UI)
        }

        val typeFromNotification = intent.getStringExtra("type")
        val destinationFromNotification = intent.getIntExtra("destinationFromNotification", -1)
        Log.d("MainActivity", "typeFromNotification: $typeFromNotification, destinationFromNotification: $destinationFromNotification")

        if (typeFromNotification == "mensaje") {
            val receiverid = intent.getStringExtra("receiverId")
            val receiverName = intent.getStringExtra("receiverName")

            val intent = Intent(this, ChatsActivity::class.java).apply {
                putExtra("receiverId", receiverid)
                putExtra("receiverName", receiverName)
            }

            startActivity(intent)
        }


        if (typeFromNotification == "solicitud_cupo") {
            viewModel.switchToDriver()
        }

        lifecycleScope.launch {
            viewModel.isPassengerMode.collectLatest { isPassengerMode ->
                setupNavigation(isPassengerMode)

                if (typeFromNotification == "aceptado" || typeFromNotification == "rechazado" || destinationFromNotification == R.id.passengerRequestsFragment) {
                    navController.addOnDestinationChangedListener(object : NavController.OnDestinationChangedListener {
                        override fun onDestinationChanged(
                            controller: NavController,
                            destination: androidx.navigation.NavDestination,
                            arguments: Bundle?
                        ) {
                            controller.removeOnDestinationChangedListener(this)
                            try {
                                controller.navigate(R.id.passengerRequestsFragment)
                                intent.removeExtra("destinationFromNotification")
                                Log.d("MainActivity", "Navegando a destino desde notificación: $destinationFromNotification")
                            } catch (e: Exception) {
                                Log.e("MainActivity", "Error al navegar desde notificación", e)
                            }
                        }
                    })
                }else if(typeFromNotification == "viaje_iniciado" || typeFromNotification == "viaje_terminado" || typeFromNotification == "viaje_cancelado" ||destinationFromNotification == R.id.passengerHomeFragment ){
                    navController.addOnDestinationChangedListener(object : NavController.OnDestinationChangedListener {
                        override fun onDestinationChanged(
                            controller: NavController,
                            destination: androidx.navigation.NavDestination,
                            arguments: Bundle?
                        ) {
                            controller.removeOnDestinationChangedListener(this)
                            try {
                                controller.navigate(R.id.passengerHomeFragment)
                                intent.removeExtra("destinationFromNotification")
                                Log.d("MainActivity", "Navegando a destino desde notificación: $destinationFromNotification")
                            } catch (e: Exception) {
                                Log.e("MainActivity", "Error al navegar desde notificación", e)
                            }
                        }
                    })
                }
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
                    FirebaseAuth.getInstance().signOut()
                    startActivity(Intent(this, AuthActivity::class.java))
                    finish()
                    return@setNavigationItemSelectedListener true
                }
                DrawerOption.Chats.id -> {
                    val intent = Intent(this, ChatsActivity::class.java)
                    startActivity(intent)
                    binding.drawerLayout.closeDrawer(GravityCompat.START)
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
    override fun onDestroy() {
        super.onDestroy()
        //dejar de escuchar cambios en el acelerómetro
        sensorManager.unregisterListener(sensorListener)
    }

    fun switchToPassengerMode() {
        viewModel.switchToPassenger()
    }


}
