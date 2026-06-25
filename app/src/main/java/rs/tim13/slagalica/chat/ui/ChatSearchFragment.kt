package rs.tim13.slagalica.chat.ui

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import androidx.core.os.bundleOf
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import rs.tim13.slagalica.R
import rs.tim13.slagalica.core.ui.BaseFragment
import rs.tim13.slagalica.databinding.FragmentChatSearchBinding

/** Pretraga korisnika iz istog regiona po regex izrazu nad username-om (spec 8). */
class ChatSearchFragment : BaseFragment<FragmentChatSearchBinding>(FragmentChatSearchBinding::inflate) {

    private val viewModel: ChatSearchViewModel by viewModels()
    private lateinit var adapter: ChatUserAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = ChatUserAdapter { user ->
            findNavController().navigate(
                R.id.action_chatSearch_to_chatConversation,
                bundleOf(
                    ChatConversationFragment.ARG_USER_ID to user.id,
                    ChatConversationFragment.ARG_USERNAME to user.username
                )
            )
        }
        binding.rvSearchResults.layoutManager = LinearLayoutManager(requireContext())
        binding.rvSearchResults.adapter = adapter

        binding.etSearchPattern.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
            override fun afterTextChanged(s: Editable?) {
                viewModel.search(requireContext(), s?.toString().orEmpty())
            }
        })

        viewModel.results.observe(viewLifecycleOwner) { list ->
            adapter.submitList(list)
            binding.tvSearchEmptyState.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
        }
        viewModel.error.observe(viewLifecycleOwner) { message ->
            if (message != null) showError(message)
        }
    }
}
