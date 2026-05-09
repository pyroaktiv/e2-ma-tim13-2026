package rs.tim13.slagalica.auth.ui

import android.os.Bundle
import android.view.View
import rs.tim13.slagalica.R
import rs.tim13.slagalica.core.ui.BaseFragment
import rs.tim13.slagalica.databinding.FragmentAuthResetPasswordBinding

class ResetPasswordFragment : BaseFragment<FragmentAuthResetPasswordBinding>(
    FragmentAuthResetPasswordBinding::inflate
){
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnConfirmReset.setOnClickListener {
            val usernameEmail = binding.tilUsernameEmail.editText?.text.toString()
            val oldPassword = binding.tilOldPassword.editText?.text.toString()
            val newPassword = binding.tilNewPassword.editText?.text.toString()
            val repeatedPassword = binding.tilRepeatPassword.editText?.text.toString()

            if (usernameEmail.isBlank() || oldPassword.isBlank() || newPassword.isBlank() || repeatedPassword.isBlank()) {
                showError(getString(R.string.all_fields_are_mandatory))
            }
            else if (newPassword != repeatedPassword) {
                showError(getString(R.string.passwords_do_not_match_error))
            }
            else {
                TODO()
            }
        }
    }
}