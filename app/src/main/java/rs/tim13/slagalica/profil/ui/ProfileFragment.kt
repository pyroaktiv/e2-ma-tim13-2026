package rs.tim13.slagalica.profil.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import kotlinx.coroutines.launch
import rs.tim13.slagalica.R
import rs.tim13.slagalica.auth.ui.AuthActivity
import rs.tim13.slagalica.core.network.RetrofitClient
import rs.tim13.slagalica.core.ui.BaseFragment
import rs.tim13.slagalica.core.util.TokenManager
import rs.tim13.slagalica.databinding.FragmentProfilBinding

class ProfilFragment : BaseFragment<FragmentProfilBinding>(FragmentProfilBinding::inflate) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnLogout.setOnClickListener { logout() }
        binding.btnChangePassword.setOnClickListener {
            findNavController().navigate(R.id.action_profil_to_resetPassword)
        }
    }

    private fun logout() {
        viewLifecycleOwner.lifecycleScope.launch {
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
