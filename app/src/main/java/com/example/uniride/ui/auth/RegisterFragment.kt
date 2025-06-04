package com.example.uniride.ui.auth

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.uniride.R
import com.example.uniride.databinding.FragmentRegisterBinding
import com.example.uniride.domain.model.User
import com.example.uniride.ui.MainActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.net.URL
import java.util.Date
import javax.net.ssl.HttpsURLConnection

class RegisterFragment : Fragment() {

    private var _binding: FragmentRegisterBinding? = null
    private val binding get() = _binding!!

    private lateinit var auth: FirebaseAuth
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRegisterBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Inicializa Firebase Auth
        auth = FirebaseAuth.getInstance()

        binding.loginButton.setOnClickListener {
            findNavController().navigate(R.id.action_registerFragment_to_loginFragment)
        }

        binding.createAccountButton.setOnClickListener {
            validateAndRegisterUser()
        }
    }

    private fun validateAndRegisterUser() {
        // Verificación de datos básicos
        val email = binding.email.text.toString().trim()
        val password = binding.password.text.toString()
        val username = binding.user.text.toString().trim()

        if (!validateInputs(email, password, username)) {
            return
        }

        showLoading(true)

        scope.launch {
            try {
                val domain = email.substringAfter("@")
                val isValidUniversityDomain = validateUniversityDomain(domain)

                withContext(Dispatchers.Main) {
                    if (isValidUniversityDomain) {
                        // El dominio es válido, proceder con el registro
                        registerUser(email, username, password)
                    } else {
                        // El dominio no es válido
                        showLoading(false)
                        binding.email.error = "Solo se permiten correos Institucionales"
                        Toast.makeText(
                            requireContext(),
                            "El correo debe pertenecer a un dominio Institucional",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    showLoading(false)
                    Log.e("Register", "Error validando dominio: ${e.message}")
                    Toast.makeText(
                        requireContext(),
                        "Error verificando el dominio Institucional. Inténtalo de nuevo.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun validateInputs(email: String, password: String, username: String): Boolean {
        var valid = true

        if (email.isEmpty()) {
            binding.email.error = "El correo es obligatorio"
            valid = false
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.email.error = "El correo no es válido"
            valid = false
        } else {
            binding.email.error = null
        }

        if (password.isEmpty()) {
            binding.password.error = "La contraseña es obligatoria"
            valid = false
        } else if (password.length < 6) {
            binding.password.error = "La contraseña debe tener al menos 6 caracteres"
            valid = false
        } else {
            binding.password.error = null
        }

        if (username.isEmpty()) {
            binding.user.error = "El usuario es obligatorio"
            valid = false
        } else {
            binding.user.error = null
        }

        return valid
    }

    private suspend fun validateUniversityDomain(domain: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val url = URL("https://raw.githubusercontent.com/Hipo/university-domains-list/master/world_universities_and_domains.json")
            val connection = url.openConnection() as HttpsURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 15000
            connection.readTimeout = 15000

            val response = connection.inputStream.bufferedReader().use { it.readText() }
            val jsonArray = JSONArray(response)

            // Buscar el dominio en universidades colombianas
            for (i in 0 until jsonArray.length()) {
                val university = jsonArray.getJSONObject(i)
                val alphaCode = university.optString("alpha_two_code", "")

                // Solo verificar universidades colombianas
                if (alphaCode == "CO") {
                    val domains = university.getJSONArray("domains")
                    for (j in 0 until domains.length()) {
                        val universityDomain = domains.getString(j)
                        if (universityDomain.equals(domain, ignoreCase = true)) {
                            // Dominio encontrado y válido
                            Log.d("Register", "Dominio válido encontrado: $domain para ${university.getString("name")}")
                            return@withContext true
                        }
                    }
                }
            }

            // Si llegamos aquí, el dominio no se encontró
            Log.d("Register", "Dominio no encontrado en universidades colombianas: $domain")
            return@withContext false

        } catch (e: Exception) {
            Log.e("Register", "Error validando dominio universitario: ${e.message}")
            return@withContext false
        }
    }

    private fun registerUser(email: String, username: String, password: String) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                showLoading(false)

                if (task.isSuccessful) {
                    val uid = auth.currentUser?.uid ?: return@addOnCompleteListener
                    val db = FirebaseFirestore.getInstance()

                    val user = User(
                        id = uid,
                        username = username,
                        email = email,
                        imgProfile = "",
                        createdAt = Date()
                    )

                    // Guardar datos básicos sin token aún
                    db.collection("users").document(uid).set(user)
                        .addOnSuccessListener {
                            // Ahora obtenemos el token FCM
                            com.google.firebase.messaging.FirebaseMessaging.getInstance().token
                                .addOnCompleteListener { tokenTask ->
                                    if (tokenTask.isSuccessful) {
                                        val token = tokenTask.result

                                        // Guardamos el token en el mismo documento del usuario
                                        db.collection("users").document(uid)
                                            .update("token", token)
                                            .addOnSuccessListener {
                                                Log.d("Register", "Token guardado correctamente")
                                            }
                                            .addOnFailureListener {
                                                Log.e("Register", "Error guardando token: ${it.message}")
                                            }
                                    }
                                }

                            // Mostrar mensaje de éxito
                            Toast.makeText(
                                requireContext(),
                                "¡Registro exitoso! Bienvenido a UniRide",
                                Toast.LENGTH_SHORT
                            ).show()

                            // Ir al MainActivity
                            val intent = Intent(requireContext(), MainActivity::class.java)
                            startActivity(intent)
                            requireActivity().finish()
                        }
                        .addOnFailureListener { e ->
                            Log.e("Register", "Error al guardar datos del usuario: ${e.message}")
                            Toast.makeText(requireContext(), "Error al guardar datos: ${e.message}", Toast.LENGTH_LONG).show()
                        }

                } else {
                    Log.e("Register", "Error al registrar: ${task.exception?.localizedMessage}")

                    // Manejo de errores específicos de Firebase Auth
                    val errorMessage = when (task.exception?.message) {
                        "The email address is already in use by another account." ->
                            "Este correo ya está registrado. Usa otro o inicia sesión."
                        "The email address is badly formatted." ->
                            "El formato del correo electrónico no es válido."
                        "The given password is invalid. [ Password should be at least 6 characters ]" ->
                            "La contraseña debe tener al menos 6 caracteres."
                        else -> "Error en el registro: ${task.exception?.message}"
                    }

                    Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_LONG).show()
                }
            }
    }

    private fun showLoading(show: Boolean) {
        if (show) {
            binding.createAccountButton.isEnabled = false
            binding.createAccountButton.text = "Verificando..."
        } else {
            binding.createAccountButton.isEnabled = true
            binding.createAccountButton.text = "Crear Cuenta"
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        scope.cancel()
        _binding = null
    }
}