package rs.tim13.slagalica.core.ui

import android.os.Bundle
import android.view.View
import androidx.navigation.fragment.findNavController
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import rs.tim13.slagalica.R
import rs.tim13.slagalica.auth.data.api.ProfileResponse
import rs.tim13.slagalica.core.network.RetrofitClient
import rs.tim13.slagalica.core.util.ImageUtils
import rs.tim13.slagalica.databinding.FragmentDemoGameListBinding

class DemoGameListFragment : BaseFragment<FragmentDemoGameListBinding>(FragmentDemoGameListBinding::inflate) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnViewProfile.setOnClickListener {
            findNavController().navigate(R.id.action_list_to_profil)
        }

        binding.btnPlayOnline.setOnClickListener {
            findNavController().navigate(R.id.action_list_to_online)
        }

        binding.btnPlayMojBroj.setOnClickListener {
            findNavController().navigate(R.id.action_list_to_mojBroj)
        }

        binding.btnPlayKorakPoKorak.setOnClickListener {
            findNavController().navigate(R.id.action_list_to_korakPoKorak)
        }

        binding.btnPlayKoZnaZna.setOnClickListener {
            findNavController().navigate(R.id.action_list_to_koZnaZna)
        }

        binding.btnPlaySpojnice.setOnClickListener {
            findNavController().navigate(R.id.action_list_to_spojnice)
        }

        binding.btnPlayAsocijacije.setOnClickListener {
            findNavController().navigate(R.id.action_list_to_asocijacije)
        }

        binding.btnPlaySkocko.setOnClickListener {
            findNavController().navigate(R.id.action_list_to_skocko)
        }

        binding.btnNotifications.setOnClickListener {
            findNavController().navigate(R.id.action_list_to_notifications)
        }

        loadMenuStats()
    }

    // Tokens / stars / league visible on the home screen at all times.
    private fun loadMenuStats() {
        RetrofitClient.getAuthClient(requireContext()).getProfile().enqueue(object : Callback<ProfileResponse> {
            override fun onResponse(call: Call<ProfileResponse>, response: Response<ProfileResponse>) {
                if (view == null) return
                val p = response.body() ?: return
                binding.tvMenuStats.text =
                    "🪙 ${p.tokens}    ⭐ ${p.totalStars}    ${ImageUtils.leagueIcon(p.league.name)} ${p.league.name}"
            }

            override fun onFailure(call: Call<ProfileResponse>, t: Throwable) {}
        })
    }
}