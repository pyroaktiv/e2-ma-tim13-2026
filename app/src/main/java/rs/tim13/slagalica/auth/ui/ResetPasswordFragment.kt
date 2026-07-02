package rs.tim13.slagalica.auth.ui

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import kotlinx.coroutines.launch
import rs.tim13.slagalica.R
import rs.tim13.slagalica.auth.data.api.dto.ResetPasswordRequest
import rs.tim13.slagalica.core.network.RetrofitClient
import rs.tim13.slagalica.core.ui.BaseFragment
import rs.tim13.slagalica.databinding.FragmentAuthResetPasswordBinding

class ResetPasswordFragment : BaseFragment<FragmentAuthResetPasswordBinding>(
    FragmentAuthResetPasswordBinding::inflate
){
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnConfirmReset.setOnClickListener {
            val oldPassword = binding.tilOldPassword.editText?.text.toString()
            val newPassword = binding.tilNewPassword.editText?.text.toString()
            val repeatedPassword = binding.tilRepeatPassword.editText?.text.toString()

            if (oldPassword.isBlank() || newPassword.isBlank() || repeatedPassword.isBlank()) {
                showError(getString(R.string.all_fields_are_mandatory))
            }
            else if (newPassword != repeatedPassword) {
                showError(getString(R.string.passwords_do_not_match_error))
            }
            else {
                // Privremena direktna logika poziva
                viewLifecycleOwner.lifecycleScope.launch {
                    try {
                        binding.btnConfirmReset.isEnabled = false

                        val api = RetrofitClient.getAuthClient(requireContext())
                        val request = ResetPasswordRequest(
                            oldPassword = oldPassword,
                            newPassword = newPassword,
                            newPasswordConfirm = repeatedPassword
                        )

                        val response = api.resetPassword(request)

                        if (response.isSuccessful && response.body() != null) {
                            Toast.makeText(requireContext(), response.body()!!.message, Toast.LENGTH_LONG).show()
                            findNavController().popBackStack()
                        } else {
                            showError("Greška: Proverite trenutnu lozinku.")
                        }
                    } catch (e: Exception) {
                        showError("Greška u komunikaciji sa serverom.")
                    } finally {
                        binding.btnConfirmReset.isEnabled = true
                    }
                }
            }
        }
    }
}