package com.example.uniride

import android.os.Bundle

import androidx.appcompat.app.AppCompatActivity
import com.example.uniride.adapters.UserAdapter
import com.example.uniride.databinding.ActivityDriverRateBinding
import com.example.uniride.model.User

class DriverRateActivity : AppCompatActivity() {
    private lateinit var binding: ActivityDriverRateBinding
    private val usersComent = listOf(
        User("Joseph Guerra", "Buen Conductor, llegó rápido.", R.drawable.ic_profile),
        User("Laura Guevara", "Tardó 10 minutos en llegar.", R.drawable.ic_profile),
        User("Esteven Hernandéz", "No llegó", R.drawable.ic_profile)
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDriverRateBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val adapter = UserAdapter(this, usersComent)
        binding.userData.adapter = adapter
    }

}