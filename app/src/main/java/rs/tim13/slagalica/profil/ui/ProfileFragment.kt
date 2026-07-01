package rs.tim13.slagalica.profil.ui

import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.View
import android.widget.HorizontalScrollView
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch
import rs.tim13.slagalica.R
import rs.tim13.slagalica.auth.ui.AuthActivity
import rs.tim13.slagalica.core.network.RetrofitClient
import rs.tim13.slagalica.core.ui.BaseFragment
import rs.tim13.slagalica.core.util.ProfileResources
import rs.tim13.slagalica.core.util.QrCodes
import rs.tim13.slagalica.core.util.TokenManager
import rs.tim13.slagalica.databinding.FragmentProfilBinding
import rs.tim13.slagalica.profil.data.api.ProfileApiService
import rs.tim13.slagalica.profil.data.api.dto.AvatarUpdateRequest
import rs.tim13.slagalica.profil.data.api.dto.ProfileDto
import rs.tim13.slagalica.profil.data.api.dto.StatsDto
import kotlin.math.roundToInt

/**
 * Profil registrovanog igrača (spec 2): podaci, avatar sa okvirom lige, QR kod za poziv
 * prijatelja i per-game statistika. Podaci se učitavaju sa `/api/user/profile` i
 * `/api/user/stats`, a avatar menja preko `PUT /api/user/avatar`.
 */
class ProfilFragment : BaseFragment<FragmentProfilBinding>(FragmentProfilBinding::inflate) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnLogout.setOnClickListener { logout() }
        binding.btnChangePassword.setOnClickListener {
            findNavController().navigate(R.id.action_profil_to_resetPassword)
        }
        binding.fabEditAvatar.setOnClickListener { showAvatarPicker() }
        binding.avatarFrame.setOnClickListener { showAvatarPicker() }

        if (TokenManager(requireContext()).getToken() == null) {
            showError(getString(R.string.profile_guest_only))
            return
        }
        loadProfile()
        loadStats()
    }

    private fun api(): ProfileApiService =
        RetrofitClient.getClient(requireContext()).create(ProfileApiService::class.java)

    private fun loadProfile() {
        viewLifecycleOwner.lifecycleScope.launch {
            val profile = runCatching {
                val response = api().getProfile()
                if (response.isSuccessful) response.body() else null
            }.getOrNull()

            if (profile == null) {
                showError(getString(R.string.profile_load_error))
                return@launch
            }
            bindProfile(profile)
        }
    }

    private fun bindProfile(p: ProfileDto) {
        binding.tvUsername.text = p.username
        binding.tvEmail.text = p.email
        binding.tvRegion.text = getString(R.string.profile_region, p.region)
        binding.tvTokens.text = p.tokens.toString()
        binding.tvStars.text = p.totalStars.toString()
        binding.tvLeague.text = p.league.name
        binding.ivLeagueIcon.setImageResource(ProfileResources.leagueIcon(p.league.icon))
        binding.ivAvatar.setImageResource(ProfileResources.avatarDrawable(p.avatar))
        // Okvir: zlato/srebro/bronza ako je region bio top-3 prošlog ciklusa (spec 5.e), inače boja lige.
        val frameColor = ProfileResources.avatarFrameColor(p.avatarFrame)
            ?: ProfileResources.leagueColor(p.league.icon)
        binding.avatarFrame.backgroundTintList = ColorStateList.valueOf(frameColor)
        QrCodes.encode(p.qrToken)?.let { binding.ivQrCode.setImageBitmap(it) }
    }

    private fun loadStats() {
        viewLifecycleOwner.lifecycleScope.launch {
            val stats = runCatching {
                val response = api().getStats()
                if (response.isSuccessful) response.body() else null
            }.getOrNull() ?: return@launch
            bindStats(stats)
        }
    }

    private fun bindStats(s: StatsDto) {
        val o = s.overall
        binding.tvTotalGames.text = o.totalGames.toString()

        val winPct = pct(o.wins, o.totalGames)
        binding.tvWinLossRatio.text =
            if (o.totalGames > 0) "${fmt(winPct)} / ${fmt(pct(o.losses, o.totalGames))}" else "—"
        setBar(binding.pbWinLoss, winPct)

        val kzzTotal = s.koZnaZna.correct + s.koZnaZna.missed
        binding.tvKoZnaZnaStats.text = complementary(s.koZnaZna.correct, kzzTotal)
        setBar(binding.pbKoZnaZna, pct(s.koZnaZna.correct, kzzTotal))

        binding.tvSpojniceStats.text = single(s.spojnice.successful, s.spojnice.total)
        setBar(binding.pbSpojnice, pct(s.spojnice.successful, s.spojnice.total))

        binding.tvMojBrojStats.text = single(s.mojBroj.exactHits, s.mojBroj.totalAttempts)
        setBar(binding.pbMojBroj, pct(s.mojBroj.exactHits, s.mojBroj.totalAttempts))

        val kpkSolved = s.korakPoKorak.guessedAtStep.sum()
        val kpkTotal = kpkSolved + s.korakPoKorak.failed
        binding.tvKorakPoKorakStats.text = single(kpkSolved, kpkTotal)
        setBar(binding.pbKorakPoKorak, pct(kpkSolved, kpkTotal))
        binding.tvKorakPoKorakBreakdown.text =
            breakdown(getString(R.string.profile_breakdown_step), s.korakPoKorak.guessedAtStep)

        val asocTotal = s.asocijacije.solved + s.asocijacije.unsolved
        binding.tvAsocijacijeStats.text = complementary(s.asocijacije.solved, asocTotal)
        setBar(binding.pbAsocijacije, pct(s.asocijacije.solved, asocTotal))

        val skSolved = s.skocko.correctAtAttempt.sum()
        val skTotal = skSolved + s.skocko.failed
        binding.tvSkockoStats.text = single(skSolved, skTotal)
        setBar(binding.pbSkocko, pct(skSolved, skTotal))
        binding.tvSkockoBreakdown.text =
            breakdown(getString(R.string.profile_breakdown_attempt), s.skocko.correctAtAttempt)
    }

    private val srLocale = java.util.Locale("sr", "RS")

    private fun pct(part: Int, total: Int): Double = if (total > 0) part * 100.0 / total else 0.0

    private fun fmt(value: Double): String = String.format(srLocale, "%.1f%%", value)

    private fun single(part: Int, total: Int): String = if (total > 0) fmt(pct(part, total)) else "—"

    /** Komplementarni odnos koji UVEK sabira na 100% (npr. tačno/netačno, rešene/nerešene). */
    private fun complementary(part: Int, total: Int): String {
        if (total <= 0) return "—"
        val first = pct(part, total)
        return "${fmt(first)} / ${fmt(100.0 - first)}"
    }

    private fun setBar(bar: android.widget.ProgressBar, value: Double) {
        bar.progress = value.roundToInt().coerceIn(0, 100)
    }

    /** Prikaz broja pogodaka po koraku/pokušaju (spec 2.c.iv i 2.c.vi). */
    private fun breakdown(label: String, counts: List<Int>): String =
        label + " " + counts.mapIndexed { i, c -> "${i + 1}:$c" }.joinToString("  ")

    private fun showAvatarPicker() {
        if (TokenManager(requireContext()).getToken() == null) return
        val ctx = requireContext()
        val density = resources.displayMetrics.density
        val cell = (64 * density).toInt()
        val gap = (6 * density).toInt()
        val pad = (16 * density).toInt()

        // Jedan horizontalni red avatara koji se skroluje udesno.
        val row = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(pad, pad, pad, pad)
        }

        lateinit var dialog: AlertDialog
        ProfileResources.selectableAvatars.forEach { name ->
            val image = ImageView(ctx).apply {
                setImageResource(ProfileResources.avatarDrawable(name))
                layoutParams = LinearLayout.LayoutParams(cell, cell).apply {
                    setMargins(gap, gap, gap, gap)
                }
                isClickable = true
                setOnClickListener {
                    dialog.dismiss()
                    changeAvatar(name)
                }
            }
            row.addView(image)
        }

        val scroll = HorizontalScrollView(ctx).apply { addView(row) }

        dialog = MaterialAlertDialogBuilder(ctx)
            .setTitle(R.string.profile_choose_avatar)
            .setView(scroll)
            .setNegativeButton(R.string.common_cancel, null)
            .create()
        dialog.show()
    }

    private fun changeAvatar(name: String) {
        viewLifecycleOwner.lifecycleScope.launch {
            val ok = runCatching { api().updateAvatar(AvatarUpdateRequest(name)).isSuccessful }
                .getOrDefault(false)
            if (ok) {
                binding.ivAvatar.setImageResource(ProfileResources.avatarDrawable(name))
            } else {
                showError(getString(R.string.profile_avatar_error))
            }
        }
    }

    private fun logout() {
        viewLifecycleOwner.lifecycleScope.launch {
            // Odjavi FCM token dok JWT još važi (spec 11) — da ugašen nalog ne prima notifikacije.
            runCatching {
                rs.tim13.slagalica.notifications.data.FcmTokenRegistrar.unregisterCurrent(requireContext())
            }
            runCatching {
                RetrofitClient.getAuthClient(requireContext()).logout()
            }
            TokenManager(requireContext()).clearToken()

            val intent = Intent(requireContext(), AuthActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            startActivity(intent)
        }
    }
}
