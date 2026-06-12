package rs.tim13.slagalica.core.ui

import android.os.Bundle
import android.view.View
import androidx.navigation.fragment.findNavController
import rs.tim13.slagalica.R
import rs.tim13.slagalica.core.util.TokenManager
import rs.tim13.slagalica.databinding.FragmentDemoGameListBinding

class DemoGameListFragment : BaseFragment<FragmentDemoGameListBinding>(FragmentDemoGameListBinding::inflate) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val tokenManager = TokenManager(requireContext())

        val isLoggedIn = tokenManager.getToken() != null

        if (isLoggedIn) {
            binding.btnChangePassword.visibility = View.VISIBLE
            binding.btnChangePassword.setOnClickListener {
                findNavController().navigate(R.id.action_list_to_reset_password)
            }
        } else {
            binding.btnChangePassword.visibility = View.GONE
        }

        binding.btnViewProfile.setOnClickListener {
            findNavController().navigate(R.id.action_list_to_profil)
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
    }
}