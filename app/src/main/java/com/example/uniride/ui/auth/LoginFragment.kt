package com.example.uniride.ui.auth

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.uniride.R
import com.example.uniride.connection.SupabaseInstance
import com.example.uniride.databinding.FragmentLoginBinding
import com.example.uniride.ui.MainActivity
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import kotlinx.coroutines.launch
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey



class LoginFragment : Fragment() {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!
    private lateinit var securePrefs: SharedPreferences
    private lateinit var masterKey: MasterKey

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        //Se inicializa EncryptedSharedPreferences
        context?.let {
            masterKey = MasterKey.Builder(it)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

            securePrefs = EncryptedSharedPreferences.create(
                it,
                "secure_user_prefs",
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        }

        //Va a al fragmento de registrar usuario
        binding.registerButton.setOnClickListener {
            findNavController().navigate(R.id.action_loginFragment_to_registerFragment)
        }

        binding.ingresarButton.setOnClickListener {
            val email = binding.email.text.toString()
            val password = binding.password.text.toString()

            //validar si los campos obligatorios ya se completaron
            var valid = true
            if (email.isEmpty()) {
                binding.email.error = "El correo es obligatorio"
                valid = false
            } else {
                binding.email.error = null
            }

            if (password.isEmpty()) {
                binding.password.error = "La contraseña es obligatoria"
                valid = false
            } else {
                binding.password.error = null
            }

            if (!valid) return@setOnClickListener

            lifecycleScope.launch {
                loginUser(email, password)
            }
        }

        binding.olvidoText.setOnClickListener {
            findNavController().navigate(R.id.action_loginFragment_to_recoverPasswordFragment)
        }
    }
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
    //Una función de tipo suspend es una función que puede ser pausada y
    //reanudada sin bloquear el hilo en el que se ejecuta. Permitiendo
    //escrbir código asíncrono, cosa necesaria para utilizar supabase

    private suspend fun loginUser(email: String, password: String) {
        try {
            SupabaseInstance.client.auth.signInWith(Email) {
                this.email = email
                this.password = password
            }

            securePrefs.edit()
                .putString("user_email", email)
                .putString("user_password", password)
                .apply()

            Log.i("Login", "Se inició sesión correctamente")
            startActivity(Intent(requireContext(), MainActivity::class.java))
            requireActivity().finish()

        } catch (e: Exception) {
            Log.e("Login", "Error al iniciar sesión: ${e.localizedMessage}")
            Toast.makeText(requireContext(), "Correo o contraseña incorrectos", Toast.LENGTH_SHORT).show()
        }
    }
}
