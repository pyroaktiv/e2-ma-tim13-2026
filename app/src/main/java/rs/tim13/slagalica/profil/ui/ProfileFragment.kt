package rs.tim13.slagalica.profil.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import rs.tim13.slagalica.auth.data.api.ProfileResponse
import rs.tim13.slagalica.auth.data.api.StatsResponse
import rs.tim13.slagalica.auth.ui.AuthActivity
import rs.tim13.slagalica.core.network.RetrofitClient
import rs.tim13.slagalica.core.ui.BaseFragment
import rs.tim13.slagalica.core.util.TokenManager
import rs.tim13.slagalica.databinding.FragmentProfilBinding
import rs.tim13.slagalica.online.GameClient
import kotlin.math.roundToInt

class ProfilFragment : BaseFragment<FragmentProfilBinding>(FragmentProfilBinding::inflate) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.fabEditAvatar.setOnClickListener {
            Toast.makeText(requireContext(), "Izmena avatara — uskoro", Toast.LENGTH_SHORT).show()
        }

        binding.btnLogout.setOnClickListener { logout() }

        loadProfile()
        loadStats()
    }

    private fun api() = RetrofitClient.getAuthClient(requireContext())

    private fun loadProfile() {
        api().getProfile().enqueue(object : Callback<ProfileResponse> {
            override fun onResponse(call: Call<ProfileResponse>, response: Response<ProfileResponse>) {
                if (!isAdded || _binding() == null) return
                val p = response.body() ?: return
                binding.tvUsername.text = p.username
                binding.tvEmail.text = p.email
                binding.tvRegion.text = "Region: ${p.region}"
                binding.tvLeague.text = p.league.name
                binding.tvStars.text = p.totalStars.toString()
                binding.tvTokens.text = p.tokens.toString()
            }

            override fun onFailure(call: Call<ProfileResponse>, t: Throwable) {
                if (isAdded) showError("Profil nije učitan: ${t.message}")
            }
        })
    }

    private fun loadStats() {
        api().getStats().enqueue(object : Callback<StatsResponse> {
            override fun onResponse(call: Call<StatsResponse>, response: Response<StatsResponse>) {
                if (!isAdded || _binding() == null) return
                val s = response.body() ?: return
                val winPct = (s.overall.winRatio * 100).roundToInt()
                binding.tvTotalGames.text = s.overall.totalGames.toString()
                binding.tvWinLossRatio.text =
                    "${s.overall.wins} / ${s.overall.losses}  ($winPct%)"
                binding.tvKoZnaZnaStats.text =
                    "tačno ${s.koZnaZna.correct} / netačno ${s.koZnaZna.missed}"
                binding.tvMojBrojStats.text =
                    pct(s.mojBroj.exactHits, s.mojBroj.totalAttempts)
                binding.tvKorakPoKorakStats.text =
                    "${s.korakPoKorak.guessedAtStep.sum()} pogodaka"
                binding.tvSpojniceStats.text =
                    pct(s.spojnice.successful, s.spojnice.total)
                binding.tvAsocijacijeStats.text =
                    "${s.asocijacije.solved} / ${s.asocijacije.unsolved}"
                binding.tvSkockoStats.text =
                    pct(s.skocko.correctAtAttempt.sum(), s.skocko.correctAtAttempt.sum() + s.skocko.failed)
            }

            override fun onFailure(call: Call<StatsResponse>, t: Throwable) {
                if (isAdded) showError("Statistika nije učitana: ${t.message}")
            }
        })
    }

    private fun pct(part: Int, total: Int): String =
        if (total <= 0) "—" else "${(part * 100.0 / total).roundToInt()}%"

    private fun logout() {
        val tm = TokenManager(requireContext())
        tm.clearToken()
        tm.clearUser()
        GameClient.disconnect()
        val intent = Intent(requireContext(), AuthActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        startActivity(intent)
        requireActivity().finish()
    }

    // BaseFragment keeps the binding private; expose a null-check via a tiny helper.
    private fun _binding(): FragmentProfilBinding? = if (view == null) null else binding
}
