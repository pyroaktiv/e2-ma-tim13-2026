package rs.tim13.slagalica.chat.ui

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import rs.tim13.slagalica.core.network.socket.SocketManager
import rs.tim13.slagalica.core.ui.BaseFragment
import rs.tim13.slagalica.databinding.FragmentChatConversationBinding

/** Real-time čet sa jednim korisnikom istog regiona (spec 8). */
class ChatConversationFragment :
    BaseFragment<FragmentChatConversationBinding>(FragmentChatConversationBinding::inflate) {

    private val viewModel: ChatConversationViewModel by viewModels()
    private lateinit var adapter: ChatMessageAdapter

    private val otherUserId: Int by lazy { requireArguments().getInt(ARG_USER_ID) }
    private val otherUsername: String by lazy { requireArguments().getString(ARG_USERNAME).orEmpty() }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        SocketManager.connect(requireContext())

        binding.tvConversationTitle.text = otherUsername

        adapter = ChatMessageAdapter(otherUserId)
        val layoutManager = LinearLayoutManager(requireContext()).apply { stackFromEnd = true }
        binding.rvMessages.layoutManager = layoutManager
        binding.rvMessages.adapter = adapter

        binding.btnSend.setOnClickListener {
            val text = binding.etMessageInput.text?.toString().orEmpty()
            if (text.isNotBlank()) {
                viewModel.sendMessage(otherUserId, text)
                binding.etMessageInput.text?.clear()
            }
        }

        viewModel.messages.observe(viewLifecycleOwner) { list ->
            adapter.submitList(list) {
                if (list.isNotEmpty()) binding.rvMessages.scrollToPosition(list.size - 1)
            }
        }
        viewModel.error.observe(viewLifecycleOwner) { message ->
            if (message != null) {
                showError(message)
                viewModel.consumeError()
            }
        }

        SocketManager.incoming.observe(viewLifecycleOwner) { message ->
            viewModel.onServerMessage(message, otherUserId)
        }

        viewModel.loadHistory(requireContext(), otherUserId)
    }

    companion object {
        const val ARG_USER_ID = "userId"
        const val ARG_USERNAME = "username"
    }
}
