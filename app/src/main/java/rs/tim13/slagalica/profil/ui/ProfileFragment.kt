package rs.tim13.slagalica.profil.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import rs.tim13.slagalica.auth.data.api.AvatarRequest
import rs.tim13.slagalica.auth.data.api.MessageResponse
import rs.tim13.slagalica.auth.data.api.ProfileResponse
import rs.tim13.slagalica.auth.data.api.StatsResponse
import rs.tim13.slagalica.auth.ui.AuthActivity
import rs.tim13.slagalica.core.network.RetrofitClient
import rs.tim13.slagalica.core.ui.BaseFragment
import rs.tim13.slagalica.core.util.ImageUtils
import rs.tim13.slagalica.core.util.TokenManager
import rs.tim13.slagalica.databinding.FragmentProfilBinding
import rs.tim13.slagalica.online.GameClient
import kotlin.math.roundToInt

class ProfilFragment : BaseFragment<FragmentProfilBinding>(FragmentProfilBinding::inflate) {

    private var username: String = "?"

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.fabEditAvatar.setOnClickListener { showAvatarPicker() }
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
                username = p.username
                binding.tvUsername.text = p.username
                binding.tvEmail.text = p.email
                binding.tvRegion.text = "Region: ${p.region}"
                binding.tvLeague.text = "${ImageUtils.leagueIcon(p.league.name)} ${p.league.name}"
                binding.tvStars.text = p.totalStars.toString()
                binding.tvTokens.text = p.tokens.toString()
                val avatarSeed = if (p.avatar == "default") p.username else p.avatar
                binding.ivAvatar.setImageBitmap(ImageUtils.generateAvatar(avatarSeed))
                runCatching { binding.ivQrCode.setImageBitmap(ImageUtils.generateQr(p.qrToken, 400)) }
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
                    "tačno ${s.koZnaZna.correct} / netačno ${s.koZnaZna.missed}${avg(s.koZnaZna.pointsSum, s.koZnaZna.plays)}"
                binding.tvMojBrojStats.text =
                    "${pct(s.mojBroj.exactHits, s.mojBroj.totalAttempts)}${avg(s.mojBroj.pointsSum, s.mojBroj.plays)}"
                binding.tvKorakPoKorakStats.text =
                    "${s.korakPoKorak.guessedAtStep.sum()} pogodaka${avg(s.korakPoKorak.pointsSum, s.korakPoKorak.plays)}"
                binding.tvSpojniceStats.text =
                    "${pct(s.spojnice.successful, s.spojnice.total)}${avg(s.spojnice.pointsSum, s.spojnice.plays)}"
                binding.tvAsocijacijeStats.text =
                    "${s.asocijacije.solved} / ${s.asocijacije.unsolved}${avg(s.asocijacije.pointsSum, s.asocijacije.plays)}"
                binding.tvSkockoStats.text =
                    "${pct(s.skocko.correctAtAttempt.sum(), s.skocko.correctAtAttempt.sum() + s.skocko.failed)}${avg(s.skocko.pointsSum, s.skocko.plays)}"
            }

            override fun onFailure(call: Call<StatsResponse>, t: Throwable) {
                if (isAdded) showError("Statistika nije učitana: ${t.message}")
            }
        })
    }

    private fun pct(part: Int, total: Int): String =
        if (total <= 0) "—" else "${(part * 100.0 / total).roundToInt()}%"

    // average points earned per game (spec FR2.c.i)
    private fun avg(pointsSum: Int, plays: Int): String =
        if (plays > 0) "  •  Ø ${(pointsSum.toDouble() / plays).roundToInt()} bod" else ""

    private fun showAvatarPicker() {
        val options = (1..10).map { "avatar_%02d".format(it) }
        AlertDialog.Builder(requireContext())
            .setTitle("Izaberi avatar")
            .setItems(options.toTypedArray()) { _, which -> updateAvatar(options[which]) }
            .setNegativeButton("Otkaži", null)
            .show()
    }

    private fun updateAvatar(avatar: String) {
        api().updateAvatar(AvatarRequest(avatar)).enqueue(object : Callback<MessageResponse> {
            override fun onResponse(call: Call<MessageResponse>, response: Response<MessageResponse>) {
                if (!isAdded || view == null) return
                if (response.isSuccessful) {
                    binding.ivAvatar.setImageBitmap(ImageUtils.generateAvatar(avatar))
                    Toast.makeText(requireContext(), "Avatar promenjen", Toast.LENGTH_SHORT).show()
                } else {
                    showError("Izmena avatara nije uspela")
                }
            }

            override fun onFailure(call: Call<MessageResponse>, t: Throwable) {
                if (isAdded) showError("Greška: ${t.message}")
            }
        })
    }

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
