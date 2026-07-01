package rs.tim13.slagalica.leaderboard.ui

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.button.MaterialButtonToggleGroup
import rs.tim13.slagalica.R
import rs.tim13.slagalica.core.ui.BaseFragment
import rs.tim13.slagalica.core.util.TokenManager
import rs.tim13.slagalica.databinding.FragmentLeaderboardBinding
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

class LeaderboardFragment : BaseFragment<FragmentLeaderboardBinding>(FragmentLeaderboardBinding::inflate) {

    private val viewModel: LeaderboardViewModel by viewModels()
    private lateinit var adapter: LeaderboardAdapter

    private val refreshHandler = Handler(Looper.getMainLooper())
    private val refreshRunnable = object : Runnable {
        override fun run() {
            viewModel.refresh()
            refreshHandler.postDelayed(this, 2 * 60 * 1000L)
        }
    }

    private val dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupToolbar()
        setupRecyclerView()
        setupTabButtons()
        observeViewModel()

        viewModel.load(0)
    }

    override fun onResume() {
        super.onResume()
        refreshHandler.postDelayed(refreshRunnable, 2 * 60 * 1000L)
    }

    override fun onPause() {
        super.onPause()
        refreshHandler.removeCallbacks(refreshRunnable)
    }

    private fun setupToolbar() {
        binding.btnBack.setOnClickListener { findNavController().popBackStack() }
    }

    private fun setupRecyclerView() {
        val userId = TokenManager(requireContext()).getUserId()
        adapter = LeaderboardAdapter(userId)
        binding.rvLeaderboard.layoutManager = LinearLayoutManager(requireContext())
        binding.rvLeaderboard.adapter = adapter
    }

    private fun setupTabButtons() {
        binding.layoutTabs.addOnButtonCheckedListener { _: MaterialButtonToggleGroup, checkedId: Int, isChecked: Boolean ->
            if (isChecked) {
                viewModel.load(if (checkedId == R.id.btnTabWeekly) 0 else 1)
            }
        }
        binding.layoutTabs.check(R.id.btnTabWeekly)
    }

    private fun observeViewModel() {
        viewModel.leaderboardState.observe(viewLifecycleOwner) { data ->
            if (data == null) return@observe

            adapter.submitList(data.entries)

            val start = parseDate(data.cycleStart)
            val end   = parseDate(data.cycleEnd)
            binding.tvCycleRange.text = "$start – $end"

            val myRank = data.myRank
            val inList = data.entries.any { it.userId == TokenManager(requireContext()).getUserId() }
            if (myRank != null && !inList) {
                binding.tvMyRank.text = getString(R.string.leaderboard_my_rank, myRank)
                binding.tvMyRank.visibility = View.VISIBLE
            } else {
                binding.tvMyRank.visibility = View.GONE
            }

            binding.tvEmpty.visibility = if (data.entries.isEmpty()) View.VISIBLE else View.GONE
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { loading ->
            binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        }

        viewModel.error.observe(viewLifecycleOwner) { err ->
            if (err != null) showError(err)
        }
    }

    private fun parseDate(iso: String): String {
        return try {
            OffsetDateTime.parse(iso).format(dateFormatter)
        } catch (e: Exception) {
            iso.take(10)
        }
    }
}
