package com.example.uniride.chats


import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.commit
import com.example.uniride.R

class ChatsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chats)

        // Solo añadir ChatsListFragment si es el primer lanzamiento
        if (savedInstanceState == null) {
            supportFragmentManager.commit {
                replace(R.id.fragment_container, ChatsListFragment())
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
        // Cierra el activity si estás en el ChatsListFragment, de lo contrario hace pop del backstack
        if (supportFragmentManager.backStackEntryCount == 0) {
            finish()
        } else {
            super.onBackPressed()
        }
    }
}
