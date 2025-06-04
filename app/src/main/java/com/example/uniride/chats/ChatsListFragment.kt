package com.example.uniride.chats

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.uniride.databinding.FragmentChatsListBinding
import com.example.uniride.domain.adapter.ChatsListAdapter
import com.example.uniride.domain.model.ChatResumen
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.example.uniride.R

class ChatsListFragment : Fragment() {

    private var _binding: FragmentChatsListBinding? = null
    private val binding get() = _binding!!

    private val db = FirebaseDatabase.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private lateinit var chatsAdapter: ChatsListAdapter
    private val chatsList = mutableListOf<ChatResumen>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChatsListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val miUid = auth.currentUser?.uid ?: return

        chatsAdapter = ChatsListAdapter(chatsList) { chat ->
            val bundle = bundleOf(
                "receiverId" to chat.receptorUid,
                "receiverName" to chat.receptorNombre
            )
            (binding.root.context as? ChatsActivity)?.openChatFragment(
                chat.receptorUid,
                chat.receptorNombre
            )

        }
        binding.btnAtras.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }

        binding.recyclerChats.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerChats.adapter = chatsAdapter

        escucharCambiosEnChats(miUid)
    }
    private fun escucharCambiosEnChats(miUid: String) {
        val chatsRef = db.getReference("chats")

        chatsRef.addChildEventListener(object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                manejarCambioDeChat(snapshot, miUid)
            }

            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
                manejarCambioDeChat(snapshot, miUid)
            }

            override fun onChildRemoved(snapshot: DataSnapshot) {}
            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onCancelled(error: DatabaseError) {}
        })
    }
    private fun manejarCambioDeChat(chatSnap: DataSnapshot, miUid: String) {
        if (!isAdded || _binding == null) return

        val chatId = chatSnap.key ?: return
        if (!chatId.contains(miUid)) return

        val mensajesSnap = chatSnap.child("mensajes")
        val ultimoMensajeSnap = mensajesSnap.children.lastOrNull()
        val ultimoMensaje = if (ultimoMensajeSnap != null) {
            ultimoMensajeSnap.child("texto").getValue(String::class.java) ?: "Sin mensaje"
        } else {
            "Sin mensajes"
        }

        val receptorUid = chatId.split("_").firstOrNull { it != miUid } ?: return

        obtenerNombreDesdeFirestore(receptorUid, ultimoMensaje, replace = true)
    }


    private fun obtenerNombreDesdeFirestore(uid: String, ultimoMensaje: String, replace: Boolean) {
        val firestore = com.google.firebase.firestore.FirebaseFirestore.getInstance()
        firestore.collection("users")
            .document(uid)
            .get()
            .addOnSuccessListener { doc ->
                val nombre = doc.getString("username") ?: "Desconocido"
                val nuevoResumen = ChatResumen(uid, nombre, ultimoMensaje)

                if (replace) {
                    val index = chatsList.indexOfFirst { it.receptorUid == uid }
                    if (index != -1) {
                        chatsList[index] = nuevoResumen
                        chatsAdapter.notifyItemChanged(index)
                    } else {
                        chatsList.add(nuevoResumen)
                        chatsAdapter.notifyItemInserted(chatsList.size - 1)
                    }
                } else {
                    chatsList.add(nuevoResumen)
                    chatsAdapter.notifyItemInserted(chatsList.size - 1)
                }
            }
    }




    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
