package rs.tim13.slagalica.auth.ui

import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import androidx.navigation.fragment.findNavController
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import rs.tim13.slagalica.R
import rs.tim13.slagalica.auth.data.api.MessageResponse
import rs.tim13.slagalica.auth.data.api.RegisterRequest
import rs.tim13.slagalica.core.network.RetrofitClient
import rs.tim13.slagalica.core.ui.BaseFragment
import rs.tim13.slagalica.databinding.FragmentAuthRegisterBinding

class RegisterFragment : BaseFragment<FragmentAuthRegisterBinding>(FragmentAuthRegisterBinding::inflate) {

    private val regions = listOf(
        "Vojvodina", "Šumadija", "Beograd", "Zapadna Srbija",
        "Istočna Srbija", "Južna Srbija", "Centralna Srbija",
    )

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        (binding.actvRegion as? AutoCompleteTextView)?.setAdapter(
            ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, regions)
        )

        binding.btnRegister.setOnClickListener { attemptRegister() }

        binding.tvGoToLogin.setOnClickListener {
            findNavController().navigate(R.id.action_register_to_login)
        }
    }

    private fun attemptRegister() {
        val email = binding.tilEmail.editText?.text?.toString()?.trim().orEmpty()
        val username = binding.tilUsername.editText?.text?.toString()?.trim().orEmpty()
        val region = binding.actvRegion.text?.toString()?.trim().orEmpty()
        val password = binding.tilPassword.editText?.text?.toString().orEmpty()
        val repeated = binding.tilRepeatPassword.editText?.text?.toString().orEmpty()

        if (email.isEmpty() || username.isEmpty() || region.isEmpty() || password.isEmpty()) {
            showError("Popunite sva polja")
            return
        }
        if (password != repeated) {
            showError(getString(R.string.passwords_do_not_match_error))
            return
        }

        binding.btnRegister.isEnabled = false
        val body = RegisterRequest(email, username, region, password, repeated)
        RetrofitClient.getAuthClient(requireContext()).register(body)
            .enqueue(object : Callback<MessageResponse> {
                override fun onResponse(call: Call<MessageResponse>, response: Response<MessageResponse>) {
                    if (!isAdded) return
                    binding.btnRegister.isEnabled = true
                    if (response.isSuccessful) {
                        showError("Registracija uspešna! Potvrdite nalog preko mejla, pa se prijavite.")
                        findNavController().navigate(R.id.action_register_to_login)
                    } else {
                        showError(parseError(response.errorBody()?.string()))
                    }
                }

                override fun onFailure(call: Call<MessageResponse>, t: Throwable) {
                    if (!isAdded) return
                    binding.btnRegister.isEnabled = true
                    showError("Greška u povezivanju: ${t.message}")
                }
            })
    }

    private fun parseError(raw: String?): String {
        if (raw.isNullOrBlank()) return "Registracija nije uspela"
        return try {
            JSONObject(raw).optString("error", "Registracija nije uspela")
        } catch (_: Exception) {
            "Registracija nije uspela"
        }
    }
}
