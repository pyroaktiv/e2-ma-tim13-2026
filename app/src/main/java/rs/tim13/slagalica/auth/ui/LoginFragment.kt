package rs.tim13.slagalica.auth.ui

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.kviz.auth.ui.RegisterFragment
import rs.tim13.slagalica.core.ui.BaseFragment
import rs.tim13.slagalica.databinding.FragmentAuthLoginBinding

class LoginFragment : BaseFragment<FragmentAuthLoginBinding>(FragmentAuthLoginBinding::inflate) {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnLogin.setOnClickListener { TODO() }

        binding.tvGoToRegister.setOnClickListener {
            (requireActivity() as AuthActivity).replaceFragment(RegisterFragment())
        }

        binding.tvForgotPassword.setOnClickListener {
            (requireActivity() as AuthActivity).replaceFragment(ResetPasswordFragment())
        }
    }
}