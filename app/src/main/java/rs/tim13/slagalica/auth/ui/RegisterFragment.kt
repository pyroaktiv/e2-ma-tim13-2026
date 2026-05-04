package rs.tim13.slagalica.auth.ui

import android.os.Bundle
import android.view.View
import androidx.navigation.fragment.findNavController
import rs.tim13.slagalica.R
import rs.tim13.slagalica.core.ui.BaseFragment
import rs.tim13.slagalica.databinding.FragmentAuthRegisterBinding

class RegisterFragment : BaseFragment<FragmentAuthRegisterBinding>(FragmentAuthRegisterBinding::inflate) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnRegister.setOnClickListener {
            val password = binding.tilPassword.editText?.text.toString()
            val repeatedPassword = binding.tilRepeatPassword.editText?.text.toString()
            if (password != repeatedPassword) {
                showError(getString(R.string.passwords_do_not_match_error))
            } else {
                TODO()
            }
        }

        binding.tvGoToLogin.setOnClickListener {
            // Umesto popBackStack(), možemo koristiti akciju koja čisti istoriju
            findNavController().navigate(R.id.action_register_to_login)
        }
    }
}