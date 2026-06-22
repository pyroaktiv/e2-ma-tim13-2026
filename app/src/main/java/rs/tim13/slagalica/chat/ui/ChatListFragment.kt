package rs.tim13.slagalica.chat.ui

import android.os.Bundle
import android.view.View
import androidx.core.os.bundleOf
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import rs.tim13.slagalica.R
import rs.tim13.slagalica.core.network.socket.SocketManager
import rs.tim13.slagalica.core.ui.BaseFragment
import rs.tim13.slagalica.databinding.FragmentChatListBinding

class ChatListFragment : BaseFragment<FragmentChatListBinding>(FragmentChatListBinding::inflate) {

    private val viewModel: ChatListViewModel by viewModels()
    private lateinit var adapter: ChatListAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        SocketManager.connect(requireContext())

        adapter = ChatListAdapter { conversation -> openConversation(conversation.userId, conversation.username) }
        binding.rvConversations.layoutManager = LinearLayoutManager(requireContext())
        binding.rvConversations.adapter = adapter

        binding.fabNewChat.setOnClickListener {
            findNavController().navigate(R.id.action_chatList_to_chatSearch)
        }

        viewModel.conversations.observe(viewLifecycleOwner) { list ->
            adapter.submitList(list)
            binding.tvEmptyState.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
        }

        SocketManager.incoming.observe(viewLifecycleOwner) { message ->
            viewModel.onServerMessage(message, requireContext())
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refresh(requireContext())
    }

    private fun openConversation(userId: Int, username: String) {
        findNavController().navigate(
            R.id.action_chatList_to_chatConversation,
            bundleOf(
                ChatConversationFragment.ARG_USER_ID to userId,
                ChatConversationFragment.ARG_USERNAME to username
            )
        )
    }
}
