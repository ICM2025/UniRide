package com.example.uniride.chats

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.uniride.databinding.FragmentChatBinding
import com.example.uniride.domain.adapter.MensajesAdapter
import com.example.uniride.domain.model.Mensaje
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase

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
        chatId = if (miUid < receptorUid!!) "$miUid$receptorUid" else "$receptorUid$miUid"

        binding.chatTitle.text = "Chat con: $receptorNombre"

        mensajesAdapter = MensajesAdapter(listaMensajes, miUid)
        binding.recyclerMensajes.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerMensajes.adapter = mensajesAdapter

        escucharMensajes()

        binding.enviarButton.setOnClickListener {
            val texto = binding.mensajeEditText.text.toString().trim()
            if (texto.isNotEmpty()) {
                enviarMensaje(texto)
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
        ref.addChildEventListener(object : ChildEventListener {
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
        })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
