package rs.tim13.slagalica.leaderboard.ui

import android.animation.ValueAnimator
import android.media.MediaPlayer
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import rs.tim13.slagalica.R
import rs.tim13.slagalica.core.network.RetrofitClient
import rs.tim13.slagalica.databinding.DialogRewardBinding

class RewardDialogFragment : DialogFragment() {

    private var _binding: DialogRewardBinding? = null
    private val binding get() = _binding!!
    private var mediaPlayer: MediaPlayer? = null

    companion object {
        private const val ARG_RANK            = "rank"
        private const val ARG_TOKENS          = "tokens"
        private const val ARG_CYCLE           = "cycle"
        private const val ARG_NOTIFICATION_ID = "notification_id"

        fun newInstance(rank: Int, tokens: Int, cycle: String, notificationId: Long): RewardDialogFragment {
            return RewardDialogFragment().apply {
                arguments = Bundle().apply {
                    putInt(ARG_RANK, rank)
                    putInt(ARG_TOKENS, tokens)
                    putString(ARG_CYCLE, cycle)
                    putLong(ARG_NOTIFICATION_ID, notificationId)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_FRAME, android.R.style.Theme_Light_NoTitleBar_Fullscreen)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = DialogRewardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val rank           = requireArguments().getInt(ARG_RANK)
        val tokens         = requireArguments().getInt(ARG_TOKENS)
        val cycle          = requireArguments().getString(ARG_CYCLE, "weekly")
        val notificationId = requireArguments().getLong(ARG_NOTIFICATION_ID)

        val cycleLabel = if (cycle == "weekly")
            getString(R.string.leaderboard_tab_weekly).lowercase()
        else
            getString(R.string.leaderboard_tab_monthly).lowercase()

        binding.tvRewardRank.text   = getString(R.string.reward_rank, rank, cycleLabel)
        binding.tvRewardTokens.text = getString(R.string.reward_tokens, 0)

        binding.lottieReward.playAnimation()

        mediaPlayer = try {
            MediaPlayer.create(requireContext(), R.raw.reward_fanfare)?.also { it.start() }
        } catch (e: Exception) {
            null
        }

        ValueAnimator.ofInt(0, tokens).apply {
            duration = 1200
            addUpdateListener {
                binding.tvRewardTokens.text = getString(R.string.reward_tokens, it.animatedValue as Int)
            }
            start()
        }

        binding.btnDismiss.setOnClickListener {
            markAsRead(notificationId)
            dismiss()
        }
    }

    private fun markAsRead(notificationId: Long) {
        RetrofitClient.getApiService(requireContext())
            .markNotificationRead(notificationId)
            .enqueue(object : Callback<Unit> {
                override fun onResponse(call: Call<Unit>, response: Response<Unit>) {}
                override fun onFailure(call: Call<Unit>, t: Throwable) {}
            })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        mediaPlayer?.release()
        mediaPlayer = null
        _binding = null
    }
}
