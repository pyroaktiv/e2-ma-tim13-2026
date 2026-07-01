package rs.tim13.slagalica.friends.ui

import android.os.Bundle
import android.os.CountDownTimer
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.core.os.bundleOf
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.coroutines.launch
import rs.tim13.slagalica.R
import rs.tim13.slagalica.core.network.RetrofitClient
import rs.tim13.slagalica.core.network.socket.FriendSocketEvent
import rs.tim13.slagalica.core.network.socket.ServerMessage
import rs.tim13.slagalica.core.network.socket.SocketManager
import rs.tim13.slagalica.core.ui.BaseFragment
import rs.tim13.slagalica.databinding.FragmentFriendsBinding
import rs.tim13.slagalica.friends.data.api.FriendApiService
import rs.tim13.slagalica.friends.data.api.dto.FriendDto
import rs.tim13.slagalica.friends.data.api.dto.SendInviteBody
import rs.tim13.slagalica.match.MatchMode
import rs.tim13.slagalica.match.ui.MatchHostFragment

/**
 * Glavni ekran prijatelja (spec 7): lista prijatelja, pristup zahtevima i dodavanju, slanje i
 * primanje poziva za partiju. Kada se poziv prihvati, server pokreće prijateljsku partiju i šalje
 * `match_found` — tada se prelazi na [MatchHostFragment] u režimu [MatchMode.FRIEND].
 */
class FriendsFragment : BaseFragment<FragmentFriendsBinding>(FragmentFriendsBinding::inflate) {

    private lateinit var adapter: FriendsAdapter

    private var waitingDialog: AlertDialog? = null
    private var sentInviteId: Int? = null

    private var incomingDialog: AlertDialog? = null
    private var incomingInviteId: Int? = null
    private var incomingTimer: CountDownTimer? = null

    /** Sprečava da zalutao (replay) `match_found` otvori partiju bez našeg poziva/prihvata. */
    private var expectingMatch = false
    private var navigatedToMatch = false

    private fun api(): FriendApiService =
        RetrofitClient.getClient(requireContext()).create(FriendApiService::class.java)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        SocketManager.connect(requireContext())

        adapter = FriendsAdapter(onInvite = ::sendInvite, onLongPress = ::confirmRemove)
        binding.rvFriends.layoutManager = LinearLayoutManager(requireContext())
        binding.rvFriends.adapter = adapter

        binding.fabAddFriend.setOnClickListener {
            findNavController().navigate(R.id.action_friends_to_search)
        }
        binding.btnRequests.setOnClickListener {
            findNavController().navigate(R.id.action_friends_to_requests)
        }

        SocketManager.friendEvents.observe(viewLifecycleOwner) { event ->
            if (event != null) handleFriendEvent(event)
        }
        SocketManager.incoming.observe(viewLifecycleOwner) { message ->
            if (message is ServerMessage.MatchFound && expectingMatch && !navigatedToMatch) {
                goToFriendlyMatch()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        navigatedToMatch = false
        loadFriends()
        loadRequestsCount()
    }

    override fun onDestroyView() {
        incomingTimer?.cancel()
        waitingDialog?.dismiss()
        incomingDialog?.dismiss()
        super.onDestroyView()
    }

    private fun loadFriends() {
        viewLifecycleOwner.lifecycleScope.launch {
            val friends = runCatching {
                val r = api().getFriends()
                if (r.isSuccessful) r.body() else null
            }.getOrNull().orEmpty()

            adapter.submitList(friends)
            binding.tvEmptyFriends.visibility = if (friends.isEmpty()) View.VISIBLE else View.GONE
        }
    }

    private fun loadRequestsCount() {
        viewLifecycleOwner.lifecycleScope.launch {
            val count = runCatching {
                val r = api().getFriendRequests()
                if (r.isSuccessful) r.body()?.size ?: 0 else 0
            }.getOrDefault(0)
            binding.btnRequests.text =
                if (count > 0) getString(R.string.friends_requests_count, count)
                else getString(R.string.friends_requests)
        }
    }

    // region Pozivi za partiju

    private fun sendInvite(friend: FriendDto) {
        viewLifecycleOwner.lifecycleScope.launch {
            val invite = runCatching {
                val r = api().sendGameInvite(SendInviteBody(toUserId = friend.id))
                if (r.isSuccessful) r.body() else null
            }.getOrNull()

            if (invite == null) {
                showError(getString(R.string.friends_invite_failed))
                return@launch
            }
            sentInviteId = invite.id
            expectingMatch = true
            showWaitingDialog(friend.username)
        }
    }

    private fun showWaitingDialog(username: String) {
        waitingDialog?.dismiss()
        waitingDialog = AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.friends_waiting_title))
            .setMessage(getString(R.string.friends_waiting_message, username))
            .setNegativeButton(R.string.common_cancel) { _, _ -> cancelSentInvite() }
            .setCancelable(false)
            .show()
    }

    private fun cancelSentInvite() {
        val id = sentInviteId ?: return
        expectingMatch = false
        sentInviteId = null
        viewLifecycleOwner.lifecycleScope.launch {
            runCatching { api().cancelGameInvite(id) }
        }
    }

    private fun handleFriendEvent(event: FriendSocketEvent) {
        when (event) {
            is FriendSocketEvent.GameInvite -> showIncomingInvite(event)
            is FriendSocketEvent.InviteDeclined -> if (event.inviteId == sentInviteId) {
                dismissWaiting(getString(R.string.friends_invite_declined))
            }
            is FriendSocketEvent.InviteExpired -> {
                if (event.inviteId == sentInviteId) dismissWaiting(getString(R.string.friends_invite_expired))
                if (event.inviteId == incomingInviteId) dismissIncoming()
            }
            is FriendSocketEvent.InviteCancelled -> if (event.inviteId == incomingInviteId) dismissIncoming()
            is FriendSocketEvent.FriendRequestReceived -> {
                loadRequestsCount()
                showError(getString(R.string.friends_new_request, event.fromUsername))
            }
            is FriendSocketEvent.FriendRequestAccepted -> loadFriends()
            is FriendSocketEvent.InviteAccepted -> Unit // partija kreće preko match_found
        }
        SocketManager.clearFriendEvent()
    }

    private fun showIncomingInvite(event: FriendSocketEvent.GameInvite) {
        if (incomingDialog != null) return
        incomingInviteId = event.id

        incomingDialog = AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.friends_incoming_title))
            .setMessage(getString(R.string.friends_incoming_message, event.fromUsername))
            .setPositiveButton(R.string.friends_accept) { _, _ -> acceptIncoming() }
            .setNegativeButton(R.string.friends_decline) { _, _ -> declineIncoming() }
            .setCancelable(false)
            .show()

        // 10s prozor (spec 7.d) — po isteku se poziv automatski odbija na serveru.
        incomingTimer?.cancel()
        incomingTimer = object : CountDownTimer(10_000, 1_000) {
            override fun onTick(msLeft: Long) {
                incomingDialog?.setMessage(
                    getString(R.string.friends_incoming_message_countdown, event.fromUsername, msLeft / 1000)
                )
            }
            override fun onFinish() = dismissIncoming()
        }.also { it.start() }
    }

    private fun acceptIncoming() {
        val id = incomingInviteId ?: return
        expectingMatch = true
        incomingTimer?.cancel()
        viewLifecycleOwner.lifecycleScope.launch {
            val ok = runCatching { api().acceptGameInvite(id).isSuccessful }.getOrDefault(false)
            if (!ok) {
                expectingMatch = false
                showError(getString(R.string.friends_invite_failed))
            }
        }
    }

    private fun declineIncoming() {
        val id = incomingInviteId ?: return
        incomingInviteId = null
        incomingTimer?.cancel()
        viewLifecycleOwner.lifecycleScope.launch {
            runCatching { api().declineGameInvite(id) }
        }
    }

    private fun dismissWaiting(message: String) {
        waitingDialog?.dismiss()
        waitingDialog = null
        sentInviteId = null
        expectingMatch = false
        showError(message)
    }

    private fun dismissIncoming() {
        incomingTimer?.cancel()
        incomingDialog?.dismiss()
        incomingDialog = null
        incomingInviteId = null
    }

    private fun goToFriendlyMatch() {
        navigatedToMatch = true
        expectingMatch = false
        waitingDialog?.dismiss()
        incomingDialog?.dismiss()
        incomingTimer?.cancel()
        findNavController().navigate(
            R.id.action_friends_to_match,
            bundleOf(MatchHostFragment.ARG_MODE to MatchMode.FRIEND.name)
        )
    }

    // endregion

    private fun confirmRemove(friend: FriendDto) {
        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.friends_remove_title))
            .setMessage(getString(R.string.friends_remove_message, friend.username))
            .setPositiveButton(R.string.friends_remove_confirm) { _, _ ->
                viewLifecycleOwner.lifecycleScope.launch {
                    runCatching { api().removeFriend(friend.id) }
                    loadFriends()
                }
            }
            .setNegativeButton(R.string.common_cancel, null)
            .show()
    }
}
