package com.example.uniride.connection

import android.app.Application
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient

//clase para inicializar el cliente de supabase
class SupabaseApp: Application() {
    lateinit var supabase: SupabaseClient

    override fun onCreate() {
        super.onCreate()

        supabase = createSupabaseClient(
            supabaseUrl = "https://xnolteayfqqhnnhpxevu.supabase.co",
            supabaseKey = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Inhub2x0ZWF5ZnFxaG5uaHB4ZXZ1Iiwicm9sZSI6ImFub24iLCJpYXQiOjE3NDYyNzk5ODgsImV4cCI6MjA2MTg1NTk4OH0.IQBuBxOwHFGoxzd0BY_0nE0KXN-LUJ4tlCd9Xp25tQc"
        ) {
            //instalar las funciones que vamos a usar, por ahora Autenticación
            install(Auth)
        }

        //guardar supabase en un singleton o hacer que esta clase lo exponga
        SupabaseInstance.client = supabase
    }
}