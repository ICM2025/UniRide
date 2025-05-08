package com.example.uniride.domain.model

sealed class VehicleBrand(val displayName: String) {
    object Toyota : VehicleBrand("Toyota")
    object Mazda : VehicleBrand("Mazda")
    object Renault : VehicleBrand("Renault")
    object Chevrolet : VehicleBrand("Chevrolet")
    object Kia : VehicleBrand("Kia")
    object Hyundai : VehicleBrand("Hyundai")
    object Otro : VehicleBrand("Otro")

    companion object {
        fun allBrands(): List<VehicleBrand> = listOf(
            Toyota, Mazda, Renault, Chevrolet, Kia, Hyundai, Otro
        )

        fun fromDisplayName(name: String): VehicleBrand {
            return allBrands().firstOrNull { it.displayName == name } ?: Otro
        }
    }
}
