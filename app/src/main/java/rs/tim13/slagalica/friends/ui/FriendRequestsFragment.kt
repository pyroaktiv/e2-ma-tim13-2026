package rs.tim13.slagalica.friends.ui

import android.os.Bundle
import android.view.View
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.coroutines.launch
import rs.tim13.slagalica.core.network.RetrofitClient
import rs.tim13.slagalica.core.ui.BaseFragment
import rs.tim13.slagalica.databinding.FragmentFriendRequestsBinding
import rs.tim13.slagalica.friends.data.api.FriendApiService
import rs.tim13.slagalica.friends.data.api.dto.FriendRequestDto

/** Pristigli zahtevi za prijateljstvo (spec 7) — prihvatanje/odbijanje. */
class FriendRequestsFragment : BaseFragment<FragmentFriendRequestsBinding>(FragmentFriendRequestsBinding::inflate) {

    private lateinit var adapter: FriendRequestsAdapter

    private fun api(): FriendApiService =
        RetrofitClient.getClient(requireContext()).create(FriendApiService::class.java)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        adapter = FriendRequestsAdapter(onAccept = ::accept, onDecline = ::decline)
        binding.rvRequests.layoutManager = LinearLayoutManager(requireContext())
        binding.rvRequests.adapter = adapter
        load()
    }

    private fun load() {
        viewLifecycleOwner.lifecycleScope.launch {
            val requests = runCatching {
                val r = api().getFriendRequests()
                if (r.isSuccessful) r.body() else null
            }.getOrNull().orEmpty()

            adapter.submitList(requests)
            binding.tvRequestsEmpty.visibility = if (requests.isEmpty()) View.VISIBLE else View.GONE
        }
    }

    private fun accept(request: FriendRequestDto) {
        viewLifecycleOwner.lifecycleScope.launch {
            runCatching { api().acceptFriendRequest(request.id) }
            load()
        }
    }

    private fun decline(request: FriendRequestDto) {
        viewLifecycleOwner.lifecycleScope.launch {
            runCatching { api().declineFriendRequest(request.id) }
            load()
        }
    }
}
