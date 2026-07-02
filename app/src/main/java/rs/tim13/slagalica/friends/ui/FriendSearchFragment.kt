package rs.tim13.slagalica.friends.ui

import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import kotlinx.coroutines.launch
import rs.tim13.slagalica.R
import rs.tim13.slagalica.core.network.RetrofitClient
import rs.tim13.slagalica.core.ui.BaseFragment
import rs.tim13.slagalica.databinding.FragmentFriendSearchBinding
import rs.tim13.slagalica.friends.data.api.FriendApiService
import rs.tim13.slagalica.friends.data.api.dto.SearchUserDto
import rs.tim13.slagalica.friends.data.api.dto.SendFriendRequestBody

/** Dodavanje prijatelja pretragom po imenu ili skeniranjem QR koda (spec 7.b). */
class FriendSearchFragment : BaseFragment<FragmentFriendSearchBinding>(FragmentFriendSearchBinding::inflate) {

    private lateinit var adapter: FriendSearchAdapter
    private var lastQuery: String = ""

    private val qrScanLauncher = registerForActivityResult(ScanContract()) { result ->
        result.contents?.let { addByQr(it) }
    }

    private fun api(): FriendApiService =
        RetrofitClient.getClient(requireContext()).create(FriendApiService::class.java)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = FriendSearchAdapter(onAdd = ::addByUsername)
        binding.rvSearchResults.layoutManager = LinearLayoutManager(requireContext())
        binding.rvSearchResults.adapter = adapter

        binding.etSearch.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                search(binding.etSearch.text?.toString()?.trim().orEmpty())
                true
            } else {
                false
            }
        }

        binding.btnScanQr.setOnClickListener { launchScanner() }
    }

    private fun search(query: String) {
        if (query.isBlank()) return
        lastQuery = query
        viewLifecycleOwner.lifecycleScope.launch {
            val results = runCatching {
                val r = api().searchUsers(query)
                if (r.isSuccessful) r.body() else null
            }.getOrNull().orEmpty()

            adapter.submitList(results)
            binding.tvSearchEmpty.visibility = if (results.isEmpty()) View.VISIBLE else View.GONE
        }
    }

    private fun addByUsername(user: SearchUserDto) {
        viewLifecycleOwner.lifecycleScope.launch {
            val ok = runCatching {
                api().sendFriendRequest(SendFriendRequestBody(username = user.username)).isSuccessful
            }.getOrDefault(false)
            showError(getString(if (ok) R.string.friends_request_sent else R.string.friends_request_failed))
            if (ok && lastQuery.isNotBlank()) search(lastQuery) // osveži relaciju
        }
    }

    private fun addByQr(qrToken: String) {
        viewLifecycleOwner.lifecycleScope.launch {
            val ok = runCatching {
                api().sendFriendRequest(SendFriendRequestBody(qrToken = qrToken)).isSuccessful
            }.getOrDefault(false)
            showError(getString(if (ok) R.string.friends_request_sent else R.string.friends_request_failed))
        }
    }

    private fun launchScanner() {
        val options = ScanOptions()
            .setDesiredBarcodeFormats(ScanOptions.QR_CODE)
            .setPrompt(getString(R.string.friends_scan_prompt))
            .setBeepEnabled(false)
            .setOrientationLocked(false)
        qrScanLauncher.launch(options)
    }
}
