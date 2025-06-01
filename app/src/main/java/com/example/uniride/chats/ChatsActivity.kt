package com.example.uniride.chats


import android.os.Bundle

import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.commit
import com.example.uniride.R

class ChatsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chats)

        if (savedInstanceState == null) {
            // Siempre carga primero el fragmento de lista
            supportFragmentManager.commit {
                replace(R.id.fragment_container, ChatsListFragment())
            }

            // Si viene desde notificación, navega luego al chat
            val receiverId = intent.getStringExtra("receiverId")
            val receiverName = intent.getStringExtra("receiverName")

            if (receiverId != null && receiverName != null) {
                // Esperamos a que el fragmento esté montado antes de abrir el chat
                supportFragmentManager.executePendingTransactions()
                openChatFragment(receiverId, receiverName)
            }
        }
    }


    fun openChatFragment(receiverId: String, receiverName: String) {
        val chatFragment = ChatFragment().apply {
            arguments = Bundle().apply {
                putString("receiverId", receiverId)
                putString("receiverName", receiverName)
            }
        }

        supportFragmentManager.commit {
            replace(R.id.fragment_container, chatFragment)
            addToBackStack(null)
        }
    }
    override fun onBackPressed() {
        if (supportFragmentManager.backStackEntryCount > 0) {
            super.onBackPressed()
        } else {
            val fragment = supportFragmentManager.findFragmentById(R.id.fragment_container)
            if (fragment == null || fragment !is ChatsListFragment) {
                supportFragmentManager.commit {
                    replace(R.id.fragment_container, ChatsListFragment())
                }
            } else {
                finish()
            }
        }
    }


}
