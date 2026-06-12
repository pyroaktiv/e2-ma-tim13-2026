package rs.tim13.slagalica.auth.ui

import android.content.Intent
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
import rs.tim13.slagalica.core.network.RetrofitClient
import rs.tim13.slagalica.core.ui.BaseFragment
import rs.tim13.slagalica.core.ui.GameActivity
import rs.tim13.slagalica.core.util.TokenManager
import rs.tim13.slagalica.databinding.FragmentAuthLoginBinding

class LoginFragment : BaseFragment<FragmentAuthLoginBinding>(FragmentAuthLoginBinding::inflate) {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnLogin.setOnClickListener { attemptLogin() }

        binding.tvGoToRegister.setOnClickListener {
            findNavController().navigate(R.id.action_login_to_register)
        }

        binding.tvForgotPassword.setOnClickListener {
            findNavController().navigate(R.id.action_login_to_resetPassword)
        }

        binding.tvContinueAsGuest.setOnClickListener {
            goToGame()
        }
    }

    private fun attemptLogin() {
        val identifier = binding.tilUsernameEmail.editText?.text?.toString()?.trim().orEmpty()
        val password = binding.tilPassword.editText?.text?.toString().orEmpty()

        if (identifier.isEmpty() || password.isEmpty()) {
            showError("Unesite korisničko ime/email i lozinku")
            return
        }

        binding.btnLogin.isEnabled = false
        val api = RetrofitClient.getAuthClient(requireContext())
        api.login(LoginRequest(identifier, password)).enqueue(object : Callback<LoginResponse> {
            override fun onResponse(call: Call<LoginResponse>, response: Response<LoginResponse>) {
                if (!isAdded) return
                binding.btnLogin.isEnabled = true
                val body = response.body()
                if (response.isSuccessful && body != null) {
                    val tm = TokenManager(requireContext())
                    tm.saveToken(body.token)
                    tm.saveUser(body.user.id, body.user.username)
                    goToGame()
                } else {
                    showError(parseError(response.errorBody()?.string()))
                }
            }

            override fun onFailure(call: Call<LoginResponse>, t: Throwable) {
                if (!isAdded) return
                binding.btnLogin.isEnabled = true
                showError("Greška u povezivanju: ${t.message}")
            }
        })
    }

    private fun parseError(raw: String?): String {
        if (raw.isNullOrBlank()) return "Prijava nije uspela"
        return try {
            JSONObject(raw).optString("error", "Prijava nije uspela")
        } catch (_: Exception) {
            "Prijava nije uspela"
        }
    }

    private fun goToGame() {
        startActivity(Intent(requireActivity(), GameActivity::class.java))
        requireActivity().finish()
    }
}
