package com.example.uniride.ui.auth

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.uniride.R
import com.example.uniride.connection.SupabaseInstance
import com.example.uniride.databinding.FragmentRegisterBinding
import com.example.uniride.ui.MainActivity
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import kotlinx.coroutines.launch
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey


class RegisterFragment : Fragment() {

    private var _binding: FragmentRegisterBinding? = null
    private val binding get() = _binding!!
    private lateinit var securePrefs: SharedPreferences
    private lateinit var masterKey: MasterKey


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRegisterBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

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


        binding.loginButton.setOnClickListener {
            findNavController().navigate(R.id.action_registerFragment_to_loginFragment)
        }

        binding.createAccountButton.setOnClickListener {
            val email = binding.email.text.toString()
            val password = binding.password.text.toString()
            val username = binding.user.text.toString()

            var valid = true
            //se revisa si se han completado todos los campos
            if (email.isEmpty()) {
                binding.email.error = "El correo es obligatorio"
                valid = false
            } else if(!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()){
                binding.email.error = "El correo es no es válido"
                valid = false
            }else
                binding.email.error = null

            if (password.isEmpty()) {
                binding.password.error = "La contraseña es obligatoria"
                valid = false
            } else if(password.length < 6) {
                binding.password.error = "La contraseña debe tener al menos 6 caracteres"
                valid = false
            }else
                binding.password.error = null


            if(username.isEmpty()){
                binding.user.error = "El usuario es obligatorio"
                valid = false
            } else{
                binding.user.error = null
            }
            //en el caso de que no, se envia un mensaje de error
            if (!valid)
                return@setOnClickListener

            // Corrutina para ejecutar la función de login
            lifecycleScope.launch {
                registerUser(email, username, password)
            }
        }

    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
    //Una función de tipo suspend es una función que puede ser pausada y
    //reanudada sin bloquear el hilo en el que se ejecuta. Permitiendo
    //escrbir código asíncrono, cosa necesaria para utilizar supabase

    suspend fun registerUser(email1: String, username: String, password1: String) {
        try {
            //se crea el usuario con su correo, contraseña y se agrega su nombre de usuario
             SupabaseInstance.client.auth.signUpWith(Email) {
                email = email1
                password = password1
                data = buildJsonObject {
                    put("username", username)
                }
            }
            securePrefs.edit()
                .putString("user_email", email1)
                .putString("user_password", password1)
                .apply()

            Log.i("Register", "Se registro correctamente")
            //Se dirige a la actividad home de la app
            val intent = Intent(requireContext(), MainActivity::class.java)
            startActivity(intent)
            requireActivity().finish()

        } catch (e: Exception) {
            println("Error al registrar: ${e.localizedMessage}")
        }
    }

}
