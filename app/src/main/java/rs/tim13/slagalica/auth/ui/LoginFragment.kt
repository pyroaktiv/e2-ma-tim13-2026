package rs.tim13.slagalica.auth.ui

import android.os.Bundle
import android.view.View
import androidx.navigation.fragment.findNavController
import rs.tim13.slagalica.R
import rs.tim13.slagalica.core.ui.BaseFragment
import rs.tim13.slagalica.databinding.FragmentAuthLoginBinding

class LoginFragment : BaseFragment<FragmentAuthLoginBinding>(FragmentAuthLoginBinding::inflate) {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnLogin.setOnClickListener { TODO() }

        binding.tvGoToRegister.setOnClickListener {
            findNavController().navigate(R.id.action_login_to_register)
        }

        binding.tvForgotPassword.setOnClickListener {
            findNavController().navigate(R.id.action_login_to_resetPassword)
        }

        binding.tvContinueAsGuest.setOnClickListener {
            val intent = android.content.Intent(requireActivity(), rs.tim13.slagalica.core.ui.GameActivity::class.java)
            startActivity(intent)
            requireActivity().finish()
        }
    }
}