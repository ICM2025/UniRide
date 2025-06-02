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
import java.util.Date


class RegisterFragment : Fragment() {

    private var _binding: FragmentRegisterBinding? = null
    private val binding get() = _binding!!


    private lateinit var auth: FirebaseAuth

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

            //verificacion de datos
            val email = binding.email.text.toString()
            val password = binding.password.text.toString()
            val username = binding.user.text.toString()

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

            if (!valid) return@setOnClickListener

            //si todo está bien se puede registrar al usuario
            registerUser(email, username, password)
        }
    }

    private fun registerUser(email: String, username: String, password: String) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
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

                            // Ir al MainActivity
                            val intent = Intent(requireContext(), MainActivity::class.java)
                            startActivity(intent)
                            requireActivity().finish()
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(requireContext(), "Error al guardar datos: ${e.message}", Toast.LENGTH_LONG).show()
                        }

                } else {
                    Log.e("Register", "Error al registrar: ${task.exception?.localizedMessage}")
                    Toast.makeText(requireContext(), "Error: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                }
            }
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
