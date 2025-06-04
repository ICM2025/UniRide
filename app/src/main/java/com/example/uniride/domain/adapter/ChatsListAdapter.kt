package com.example.uniride.domain.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.uniride.R
import com.example.uniride.domain.model.ChatResumen

class ChatsListAdapter(
    private val chats: List<ChatResumen>,
    private val onChatClick: (ChatResumen) -> Unit
) : RecyclerView.Adapter<ChatsListAdapter.ChatViewHolder>() {

    class ChatViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val nombre: TextView = view.findViewById(R.id.chatNombre)
        val preview: TextView = view.findViewById(R.id.chatPreview)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_chat_resumen, parent, false)
        return ChatViewHolder(view)
    }

    override fun onBindViewHolder(holder: ChatViewHolder, position: Int) {
        val chat = chats[position]
        holder.nombre.text = chat.receptorNombre
        holder.preview.text = chat.ultimoMensaje
        holder.itemView.setOnClickListener { onChatClick(chat) }
    }

    override fun getItemCount(): Int = chats.size
}

