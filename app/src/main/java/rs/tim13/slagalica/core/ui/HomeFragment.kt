package rs.tim13.slagalica.core.ui

import android.os.Bundle
import android.view.View
import androidx.core.os.bundleOf
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import kotlinx.coroutines.launch
import rs.tim13.slagalica.R
import rs.tim13.slagalica.core.network.RetrofitClient
import rs.tim13.slagalica.core.util.TokenManager
import rs.tim13.slagalica.databinding.FragmentHomeBinding
import rs.tim13.slagalica.match.MatchMode
import rs.tim13.slagalica.match.ui.MatchHostFragment
import rs.tim13.slagalica.profil.data.api.ProfileApiService
import rs.tim13.slagalica.profil.data.api.dto.ProfileDto

/**
 * Početni ekran (zamena demo liste). Pokreće partiju (online ili solo) i vodi ka funkcionalnostima
 * iz specifikacije; za sada je implementirano samo igranje partija, ostalo su placeholder-i.
 */
class HomeFragment : BaseFragment<FragmentHomeBinding>(FragmentHomeBinding::inflate) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnPlayOnline.setOnClickListener { startMatch(MatchMode.ONLINE) }
        binding.btnPlaySolo.setOnClickListener { startMatch(MatchMode.SOLO) }
        binding.btnProfile.setOnClickListener { findNavController().navigate(R.id.action_home_to_profil) }
        binding.btnNotifications.setOnClickListener { findNavController().navigate(R.id.action_home_to_notifications) }
        binding.btnChallenge.setOnClickListener { findNavController().navigate(R.id.action_home_to_challenges) }
        binding.btnChat.setOnClickListener { findNavController().navigate(R.id.action_home_to_chat) }
        binding.btnFriends.setOnClickListener { findNavController().navigate(R.id.action_home_to_friends) }
        binding.btnRegions.setOnClickListener { findNavController().navigate(R.id.action_home_to_regions) }

        loadProfile()
    }

    override fun onResume() {
        super.onResume()
        loadProfile() // osveži tokene/zvezde po povratku iz partije
    }

    private fun startMatch(mode: MatchMode) {
        findNavController().navigate(
            R.id.action_home_to_match,
            bundleOf(MatchHostFragment.ARG_MODE to mode.name)
        )
    }

    private fun loadProfile() {
        if (TokenManager(requireContext()).getToken() == null) {
            showGuest()
            return
        }
        viewLifecycleOwner.lifecycleScope.launch {
            val profile = runCatching {
                val api = RetrofitClient.getClient(requireContext()).create(ProfileApiService::class.java)
                val response = api.getProfile()
                if (response.isSuccessful) response.body() else null
            }.getOrNull()

            if (profile != null) showProfile(profile) else showGuest()
        }
    }

    private fun showProfile(profile: ProfileDto) {
        binding.tvHomeUsername.text = profile.username
        binding.tvHomeTokens.text = getString(R.string.home_tokens, profile.tokens)
        binding.tvHomeStars.text = getString(R.string.home_stars, profile.totalStars)
        binding.tvHomeLeague.text = getString(R.string.home_league, profile.league.name)
        binding.btnChallenge.isEnabled = true
        binding.btnChat.isEnabled = true
        binding.btnFriends.isEnabled = true
        binding.btnRegions.isEnabled = true
    }

    private fun showGuest() {
        binding.tvHomeUsername.text = getString(R.string.home_guest)
        binding.tvHomeTokens.text = getString(R.string.home_tokens_guest)
        binding.tvHomeStars.text = getString(R.string.home_stars_guest)
        binding.tvHomeLeague.text = ""
        binding.btnChallenge.isEnabled = false // izazov uloge zvezde/tokene, nedostupan gostu
        binding.btnChat.isEnabled = false // čet je vezan za region, nedostupan gostu
        binding.btnFriends.isEnabled = false // prijatelji zahtevaju nalog
        binding.btnRegions.isEnabled = false // region/rang zahteva nalog
    }
}
