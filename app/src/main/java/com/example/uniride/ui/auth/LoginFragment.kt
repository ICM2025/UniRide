package com.example.uniride.ui.auth

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.biometric.BiometricPrompt
import androidx.biometric.BiometricPrompt.PromptInfo
import androidx.core.content.ContextCompat
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
import java.util.concurrent.Executor
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey



class LoginFragment : Fragment() {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!
    private lateinit var biometricPrompt: BiometricPrompt
    private lateinit var promptInfo: PromptInfo
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
        // Verificar si las credenciales están guardadas
        val savedEmail = securePrefs.getString("user_email", null)
        val savedPassword = securePrefs.getString("user_password", null)

        if (!savedEmail.isNullOrEmpty() && !savedPassword.isNullOrEmpty()) {
            // Si las credenciales están guardadas, configura y muestra la autenticación biométrica
            setupBiometricPrompt()

            // Configura y muestra el prompt para la autenticación biométrica
            promptInfo = PromptInfo.Builder()
                .setTitle("Autenticación biométrica")
                .setSubtitle("Inicia sesión usando tu huella")
                .setNegativeButtonText("Cancelar")
                .setAllowedAuthenticators(androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK)
                .build()

            biometricPrompt.authenticate(promptInfo)
        } else {
            // Si no hay credenciales guardadas, no mostrar el prompt
            Log.d("LoginFragment", "No hay credenciales guardadas")
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

    private fun setupBiometricPrompt() {
        val executor: Executor = ContextCompat.getMainExecutor(requireContext())
        biometricPrompt = BiometricPrompt(this, executor, object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                val savedEmail = securePrefs.getString("user_email", null)
                val savedPassword = securePrefs.getString("user_password", null)

                if (!savedEmail.isNullOrEmpty() && !savedPassword.isNullOrEmpty()) {
                    lifecycleScope.launch {
                        loginUser(savedEmail, savedPassword)
                    }
                } else {
                    Toast.makeText(requireContext(), "No se encontraron credenciales guardadas", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                Log.e("Biometric", "Error de autenticación: $errString")
            }

            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                Log.e("Biometric", "Autenticación biométrica fallida")
            }
        })
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
    //Una función de tipo suspend es una función que puede ser pausada y
    //reanudada sin bloquear el hilo en el que se ejecuta. Permitiendo
    //escrbir código asíncrono, cosa necesaria para utilizar supabase

    suspend fun loginUser(email: String, password: String) {
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
