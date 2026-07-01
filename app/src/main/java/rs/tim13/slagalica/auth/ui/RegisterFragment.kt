package rs.tim13.slagalica.auth.ui

import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import kotlinx.coroutines.launch
import rs.tim13.slagalica.R
import rs.tim13.slagalica.auth.data.api.dto.RegisterRequest
import rs.tim13.slagalica.core.network.RetrofitClient
import rs.tim13.slagalica.core.ui.BaseFragment
import rs.tim13.slagalica.databinding.FragmentAuthRegisterBinding

class RegisterFragment : BaseFragment<FragmentAuthRegisterBinding>(FragmentAuthRegisterBinding::inflate) {

    private val regions = listOf(
        "Vojvodina",
        "Beograd",
        "Šumadija i Zapadna Srbija",
        "Južna i Istočna Srbija",
    )

    private val viewModel: AuthViewModel by viewModels {
        object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val api = RetrofitClient.getAuthClient(requireContext())
                @Suppress("UNCHECKED_CAST")
                return AuthViewModel(api) as T
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, regions)
        binding.actvRegion.setAdapter(adapter)

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.authState.collect { state ->
                when (state) {
                    is AuthState.Loading -> binding.btnRegister.isEnabled = false
                    is AuthState.RegisterSuccess -> {
                        binding.btnRegister.isEnabled = true
                        Toast.makeText(requireContext(), state.message, Toast.LENGTH_LONG).show()
                        findNavController().navigate(R.id.action_register_to_login)
                        viewModel.resetState()
                    }
                    is AuthState.Error -> {
                        binding.btnRegister.isEnabled = true
                        showError(state.message)
                        viewModel.resetState()
                    }
                    else -> binding.btnRegister.isEnabled = true
                }
            }
        }

        binding.btnRegister.setOnClickListener {
            val email = binding.tilEmail.editText?.text.toString().trim()
            val username = binding.tilUsername.editText?.text.toString().trim()
            val region = binding.actvRegion.text.toString()
            val password = binding.tilPassword.editText?.text.toString()
            val repeatedPassword = binding.tilRepeatPassword.editText?.text.toString()

            if (email.isBlank() || username.isBlank() || region.isBlank() || password.isBlank()) {
                showError(getString(R.string.all_fields_are_mandatory))
                return@setOnClickListener
            }

            if (password != repeatedPassword) {
                showError(getString(R.string.passwords_do_not_match_error))
            } else {
                viewModel.register(RegisterRequest(email, username, region, password, repeatedPassword))
            }
        }

        binding.tvGoToLogin.setOnClickListener {
            findNavController().navigate(R.id.action_register_to_login)
        }
    }
}
