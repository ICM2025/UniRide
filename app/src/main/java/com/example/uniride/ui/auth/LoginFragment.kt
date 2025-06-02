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
import com.example.uniride.databinding.FragmentLoginBinding
import com.example.uniride.ui.MainActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging


class LoginFragment : Fragment() {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    private lateinit var auth: FirebaseAuth

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = FirebaseAuth.getInstance()

        // fragmento de registro
        binding.registerButton.setOnClickListener {
            findNavController().navigate(R.id.action_loginFragment_to_registerFragment)
        }


        binding.ingresarButton.setOnClickListener {
            //verificación de datos completos
            val email = binding.email.text.toString()
            val password = binding.password.text.toString()

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

            loginUser(email, password)
        }

        // Recuperar contraseña
        binding.olvidoText.setOnClickListener {
            findNavController().navigate(R.id.action_loginFragment_to_recoverPasswordFragment)
        }
    }

    private fun loginUser(email: String, password: String) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    // Obtener userId actual
                    val userId = auth.currentUser?.uid ?: return@addOnCompleteListener

                    // Obtener token FCM
                    FirebaseMessaging.getInstance().token.addOnCompleteListener { tokenTask ->
                        if (tokenTask.isSuccessful) {
                            val token = tokenTask.result

                            // Guardar token en Firestore
                            val db = FirebaseFirestore.getInstance()
                            val userRef = db.collection("users").document(userId)
                            userRef.update("token", token)
                                .addOnSuccessListener {
                                    // Token guardado exitosamente (opcional log)
                                    Log.d("Login", "Token FCM guardado")
                                }
                                .addOnFailureListener {
                                    // Si no existe el documento, lo creamos
                                    val data = hashMapOf("token" to token)
                                    userRef.set(data)
                                }
                        }
                    }

                    // Ir a MainActivity
                    startActivity(Intent(requireContext(), MainActivity::class.java))
                    requireActivity().finish()
                } else {
                    Log.e("Login", "Error: ${task.exception?.localizedMessage}")
                    Toast.makeText(requireContext(), "Correo o contraseña incorrectos", Toast.LENGTH_SHORT).show()
                }
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
