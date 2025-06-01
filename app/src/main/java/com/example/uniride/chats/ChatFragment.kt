package com.example.uniride.chats

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.uniride.databinding.FragmentChatBinding
import com.example.uniride.domain.adapter.MensajesAdapter
import com.example.uniride.domain.model.Mensaje
import com.example.uniride.messaging.NotificationSender
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FirebaseFirestore

class ChatFragment : Fragment() {

    private var _binding: FragmentChatBinding? = null
    private val binding get() = _binding!!

    private lateinit var mensajesAdapter: MensajesAdapter
    private val listaMensajes = mutableListOf<Mensaje>()

    private val db = FirebaseDatabase.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private lateinit var chatId: String
    private var receptorUid: String? = null
    private var receptorNombre: String? = null

    private lateinit var mensajesListener: ChildEventListener

    private val db2 = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            receptorUid = it.getString("receiverId")
            receptorNombre = it.getString("receiverName")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChatBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val miUid = auth.currentUser?.uid ?: return

        // Establecer ID único para el chat entre estos dos usuarios
        chatId = if (miUid < receptorUid!!) "${miUid}_$receptorUid" else "${receptorUid}_$miUid"
        Log.d("chatid", "el chat id es: $chatId")

        binding.chatTitle.text = "Chat con: $receptorNombre"

        mensajesAdapter = MensajesAdapter(listaMensajes, miUid)
        binding.recyclerMensajes.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerMensajes.adapter = mensajesAdapter

        escucharMensajes()

        binding.enviarButton.setOnClickListener {
            val texto = binding.mensajeEditText.text.toString().trim()
            if (texto.isNotEmpty()) {
                enviarMensaje(texto)
                obtenerTokenDelReceptor(receptorUid!!){ token ->
                    if (token != null){
                        obtenerNombreDelUsuario { nombre ->
                            if(nombre != null){
                                NotificationSender.enviar(token, "mensaje", nombre, miUid, nombre, texto){
                                    success ->
                                    if(success){
                                        Log.d("mensaje", "mensaje enviado correctamente al token: $token")
                                    }else
                                        Log.e("mensaje","mensaje no fue enviado")
                                }
                            }
                        }
                    }

                }
                binding.mensajeEditText.text.clear()
            }
        }

        binding.btnBack.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }
    }

    private fun enviarMensaje(texto: String) {
        val ref = db.getReference("chats/$chatId/mensajes")
        val mensaje = Mensaje(texto, auth.currentUser!!.uid, System.currentTimeMillis())
        ref.push().setValue(mensaje)
    }

    private fun escucharMensajes() {
        val ref = db.getReference("chats/$chatId/mensajes")

        mensajesListener = object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                val mensaje = snapshot.getValue(Mensaje::class.java)
                if (mensaje != null) {
                    listaMensajes.add(mensaje)
                    mensajesAdapter.notifyItemInserted(listaMensajes.size - 1)
                    binding.recyclerMensajes.scrollToPosition(listaMensajes.size - 1)
                }
            }

            override fun onCancelled(error: DatabaseError) {}
            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onChildRemoved(snapshot: DataSnapshot) {}
            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
        }

        ref.addChildEventListener(mensajesListener)
    }


    override fun onDestroyView() {
        db.getReference("chats/$chatId/mensajes").removeEventListener(mensajesListener)
        _binding = null
        super.onDestroyView()
    }
    //para la notificaciones
    private fun obtenerTokenDelReceptor(userId: String, callback: (String?) -> Unit) {
        db2.collection("users").document(userId).get()
            .addOnSuccessListener { document ->
                val token = document.getString("token")
                callback(token)
            }
            .addOnFailureListener {
                callback(null)
            }

    }
    //para la notificación
    private fun obtenerNombreDelUsuario(callback: (String?) -> Unit) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return callback(null)
        db2.collection("users").document(userId).get()
            .addOnSuccessListener { snapshot ->
                val nombre = snapshot.getString("username")
                callback(nombre)
            }
            .addOnFailureListener {
                callback(null)
            }
    }

}
