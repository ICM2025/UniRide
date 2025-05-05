import android.content.IntentSender
import android.content.pm.PackageManager
import android.location.Location
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.location.LocationSettingsResponse
import com.google.android.gms.location.Priority
import com.google.android.gms.location.SettingsClient
import com.google.android.gms.tasks.Task

class Config_permission(
    protected val activity: AppCompatActivity,
    private val locationUpdateCallback: (Location) -> Unit
) {
    // Constants
    private val TAG = "LocationManagerBase"

    // Location components
    private lateinit var locationClient: FusedLocationProviderClient
    private lateinit var locationRequest: LocationRequest
    private lateinit var locationCallback: LocationCallback
    private var previousLocation: Location? = null
    private var lastKnownLocation: Location? = null

    // Permission launchers
    private lateinit var locationPermissionLauncher: ActivityResultLauncher<String>
    private lateinit var locationSettingsLauncher: ActivityResultLauncher<IntentSenderRequest>

    /**
     * Inicializa los componentes de localización
     */
    fun initialize() {
        initializeLocationComponents()
        registerPermissionLaunchers()
    }

    /**
     * Configura los componentes de localización
     */
    private fun initializeLocationComponents() {
        locationClient = LocationServices.getFusedLocationProviderClient(activity)
        locationRequest = createLocationRequest()
        locationCallback = createLocationCallback()
    }

    /**
     * Registra los launchers para permisos y configuraciones
     */
    private fun registerPermissionLaunchers() {
        locationPermissionLauncher = activity.registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted ->
            if (isGranted) {
                checkLocationSettings()
            } else {
                Toast.makeText(activity, "No hay permiso para acceder al GPS", Toast.LENGTH_LONG).show()
            }
        }

        locationSettingsLauncher = activity.registerForActivityResult(
            ActivityResultContracts.StartIntentSenderForResult()
        ) { result ->
            if (result.resultCode == AppCompatActivity.RESULT_OK) {
                startLocationUpdates()
            } else {
                Toast.makeText(activity, "El GPS está apagado", Toast.LENGTH_LONG).show()
            }
        }
    }

    /**
     * Verifica los permisos de ubicación
     */
    fun checkLocationPermissions() {
        if (hasLocationPermissions()) {
            requestLastLocation()
            checkLocationSettings()
        } else {
            if (ActivityCompat.shouldShowRequestPermissionRationale(
                    activity,
                    android.Manifest.permission.ACCESS_FINE_LOCATION
                )
            ) {
                Toast.makeText(
                    activity,
                    "El permiso es necesario para acceder a tu ubicación (GPS)",
                    Toast.LENGTH_LONG
                ).show()
            }
            locationPermissionLauncher.launch(android.Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    /**
     * Verifica si ya se tienen los permisos de ubicación
     */
    private fun hasLocationPermissions(): Boolean {
        return ContextCompat.checkSelfPermission(
            activity,
            android.Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(
                    activity,
                    android.Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Solicita la última ubicación conocida
     */
    private fun requestLastLocation() {
        // Verificación directa para ACCESS_FINE_LOCATION
        if (ContextCompat.checkSelfPermission(
                activity,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            locationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    lastKnownLocation = location
                    locationUpdateCallback(location)
                    Log.i(TAG, "Última ubicación: Lat=${location.latitude}, Long=${location.longitude}")
                } else {
                    Log.w(TAG, "Ubicación nula. Puede que el GPS esté apagado o no haya señal aún.")
                }
            }
        }
        // Verificación directa para ACCESS_COARSE_LOCATION (como alternativa)
        else if (ContextCompat.checkSelfPermission(
                activity,
                android.Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            locationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    lastKnownLocation = location
                    locationUpdateCallback(location)
                    Log.i(TAG, "Última ubicación: Lat=${location.latitude}, Long=${location.longitude}")
                } else {
                    Log.w(TAG, "Ubicación nula. Puede que el GPS esté apagado o no haya señal aún.")
                }
            }
        }
    }

    /**
     * Crea la configuración de la solicitud de ubicación
     */
    private fun createLocationRequest(): LocationRequest {
        return LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 10000)
            .setWaitForAccurateLocation(true)
            .setMinUpdateIntervalMillis(5000)
            .build()
    }

    /**
     * Crea el callback para las actualizaciones de ubicación
     */
    private fun createLocationCallback(): LocationCallback {
        return object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                super.onLocationResult(result)
                val loc = result.lastLocation
                if (loc != null) {
                    updateLocation(loc)
                }
            }
        }
    }

    /**
     * Verifica la configuración de ubicación del dispositivo
     */
    fun checkLocationSettings() {
        val builder = LocationSettingsRequest.Builder().addLocationRequest(locationRequest)
        val client: SettingsClient = LocationServices.getSettingsClient(activity)
        val task: Task<LocationSettingsResponse> = client.checkLocationSettings(builder.build())

        task.addOnSuccessListener {
            startLocationUpdates()
        }

        task.addOnFailureListener { exception ->
            if (exception is ResolvableApiException) {
                try {
                    val isr: IntentSenderRequest =
                        IntentSenderRequest.Builder(exception.resolution).build()
                    locationSettingsLauncher.launch(isr)
                } catch (sendEx: IntentSender.SendIntentException) {
                    Toast.makeText(activity, "No hay hardware de GPS", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    /**
     * Inicia las actualizaciones de ubicación
     */
    fun startLocationUpdates() {
        // Verificación directa de permisos
        if (ContextCompat.checkSelfPermission(
                activity,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(
                activity,
                android.Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            locationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )
        }
    }

    /**
     * Detiene las actualizaciones de ubicación
     */
    fun stopLocationUpdates() {
        locationClient.removeLocationUpdates(locationCallback)
    }

    /**
     * Actualiza la ubicación actual
     */
    private fun updateLocation(location: Location) {
        lastKnownLocation = location
        locationUpdateCallback(location)

        Log.i(TAG, "Actualización de ubicación: Lat=${location.latitude}, Long=${location.longitude}")
    }

    /**
     * Obtiene la última ubicación conocida
     */
    fun getLastKnownLocation(): Location? {
        return lastKnownLocation
    }

    /**
     * Resume el ciclo de vida
     */
    fun onResume() {
        if (hasLocationPermissions()) {
            startLocationUpdates()
        } else {
            checkLocationPermissions()
        }
    }

    /**
     * Pausa el ciclo de vida
     */
    fun onPause() {
        stopLocationUpdates()
    }
}