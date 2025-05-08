package com.example.uniride.domain.model

import com.example.uniride.R

sealed class DrawerOption(
    val id: Int,
    val title: String,
    val iconRes: Int
) {
    object Profile : DrawerOption(1, "Perfil", R.drawable.ic_profile)
    object Settings : DrawerOption(2, "Configuraciones", R.drawable.ic_settings)
    object Logout : DrawerOption(3, "Cerrar sesión", R.drawable.ic_logout)
    object About : DrawerOption(6, "Acerca de", R.drawable.ic_about)

    // Solo para conductor
    object ManageVehicles : DrawerOption(4, "Gestionar vehículos", R.drawable.ic_car)
    object Statistics : DrawerOption(5, "Estadísticas", R.drawable.ic_statistics)
}
