package com.example.uniride.ui.shared

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.uniride.R
import com.example.uniride.databinding.FragmentChatBinding
import com.example.uniride.domain.adapter.MessageAdapter
import com.example.uniride.domain.model.Message

class ChatFragment : Fragment() {

    private val args: ChatFragmentArgs by navArgs()

    companion object {
        private const val ARG_NAME = "passenger_name"

        fun newInstance(passengerName: String): ChatFragment {
            val fragment = ChatFragment()
            val args = Bundle()
            args.putString(ARG_NAME, passengerName)
            fragment.arguments = args
            return fragment
        }
    }

    private var _binding: FragmentChatBinding? = null
    private val binding get() = _binding!!

    private lateinit var passengerName: String
    private lateinit var adapter: MessageAdapter
    private val messages = mutableListOf<Message>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        passengerName = arguments?.getString(ARG_NAME) ?: "Pasajero"
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

        binding.passengerNameText.text = args.passengerName

        adapter = MessageAdapter(messages)
        binding.messagesRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.messagesRecyclerView.adapter = adapter

        binding.sendButton.setOnClickListener {
            val msg = binding.messageInput.text.toString()
            if (msg.isNotBlank()) {
                adapter.addMessage(Message(msg))
                binding.messagesRecyclerView.scrollToPosition(messages.size - 1)
                binding.messageInput.setText("")
            }
        }
        binding.btnBack.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}