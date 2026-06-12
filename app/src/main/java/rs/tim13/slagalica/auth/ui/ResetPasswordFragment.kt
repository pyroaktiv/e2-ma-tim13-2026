package rs.tim13.slagalica.auth.ui

import android.os.Bundle
import android.view.View
import androidx.navigation.fragment.findNavController
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import rs.tim13.slagalica.R
import rs.tim13.slagalica.auth.data.api.LoginRequest
import rs.tim13.slagalica.auth.data.api.LoginResponse
import rs.tim13.slagalica.auth.data.api.MessageResponse
import rs.tim13.slagalica.auth.data.api.ResetPasswordRequest
import rs.tim13.slagalica.core.network.RetrofitClient
import rs.tim13.slagalica.core.ui.BaseFragment
import rs.tim13.slagalica.core.util.TokenManager
import rs.tim13.slagalica.databinding.FragmentAuthResetPasswordBinding

class ResetPasswordFragment : BaseFragment<FragmentAuthResetPasswordBinding>(
    FragmentAuthResetPasswordBinding::inflate
) {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.btnConfirmReset.setOnClickListener { attemptReset() }
    }

    private fun attemptReset() {
        val identifier = binding.tilUsernameEmail.editText?.text?.toString()?.trim().orEmpty()
        val oldPassword = binding.tilOldPassword.editText?.text?.toString().orEmpty()
        val newPassword = binding.tilNewPassword.editText?.text?.toString().orEmpty()
        val repeated = binding.tilRepeatPassword.editText?.text?.toString().orEmpty()

        if (identifier.isBlank() || oldPassword.isBlank() || newPassword.isBlank() || repeated.isBlank()) {
            showError(getString(R.string.all_fields_are_mandatory)); return
        }
        if (newPassword != repeated) {
            showError(getString(R.string.passwords_do_not_match_error)); return
        }

        binding.btnConfirmReset.isEnabled = false
        val api = RetrofitClient.getAuthClient(requireContext())
        val tm = TokenManager(requireContext())

        // The reset endpoint requires auth, and this screen is reached before login,
        // so first authenticate with the old password, then change it.
        api.login(LoginRequest(identifier, oldPassword)).enqueue(object : Callback<LoginResponse> {
            override fun onResponse(call: Call<LoginResponse>, response: Response<LoginResponse>) {
                if (!isAdded) return
                val body = response.body()
                if (!response.isSuccessful || body == null) {
                    binding.btnConfirmReset.isEnabled = true
                    showError("Pogrešno korisničko ime ili trenutna lozinka")
                    return
                }
                tm.saveToken(body.token)
                doReset(api, tm, oldPassword, newPassword, repeated)
            }

            override fun onFailure(call: Call<LoginResponse>, t: Throwable) {
                if (!isAdded) return
                binding.btnConfirmReset.isEnabled = true
                showError("Greška u povezivanju: ${t.message}")
            }
        })
    }

    private fun doReset(
        api: rs.tim13.slagalica.auth.data.api.AuthApiService,
        tm: TokenManager,
        old: String,
        new: String,
        repeated: String,
    ) {
        api.resetPassword(ResetPasswordRequest(old, new, repeated)).enqueue(object : Callback<MessageResponse> {
            override fun onResponse(call: Call<MessageResponse>, response: Response<MessageResponse>) {
                // don't keep the user logged in from a reset
                tm.clearToken(); tm.clearUser()
                if (!isAdded) return
                binding.btnConfirmReset.isEnabled = true
                if (response.isSuccessful) {
                    showError("Lozinka je promenjena. Prijavite se novom lozinkom.")
                    findNavController().popBackStack()
                } else {
                    showError(parseError(response.errorBody()?.string()))
                }
            }

            override fun onFailure(call: Call<MessageResponse>, t: Throwable) {
                tm.clearToken()
                if (!isAdded) return
                binding.btnConfirmReset.isEnabled = true
                showError("Greška u povezivanju: ${t.message}")
            }
        })
    }

    private fun parseError(raw: String?): String {
        if (raw.isNullOrBlank()) return "Promena lozinke nije uspela"
        return try {
            JSONObject(raw).optString("error", "Promena lozinke nije uspela")
        } catch (_: Exception) {
            "Promena lozinke nije uspela"
        }
    }
}
