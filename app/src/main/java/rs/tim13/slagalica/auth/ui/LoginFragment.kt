package rs.tim13.slagalica.auth.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import kotlinx.coroutines.launch
import rs.tim13.slagalica.R
import rs.tim13.slagalica.auth.data.api.dto.LoginRequest
import rs.tim13.slagalica.core.network.RetrofitClient
import rs.tim13.slagalica.core.ui.BaseFragment
import rs.tim13.slagalica.core.ui.GameActivity
import rs.tim13.slagalica.core.util.TokenManager
import rs.tim13.slagalica.databinding.FragmentAuthLoginBinding

class LoginFragment : BaseFragment<FragmentAuthLoginBinding>(FragmentAuthLoginBinding::inflate) {

    private val viewModel: AuthViewModel by viewModels {
        object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val api = RetrofitClient.getAuthClient(requireContext())
                return AuthViewModel(api) as T
            }
        }
    }
    private lateinit var tokenManager: TokenManager

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        tokenManager = TokenManager(requireContext())
        val api = RetrofitClient.getAuthClient(requireContext())

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.authState.collect { state ->
                when (state) {
                    is AuthState.Loading -> {
                        binding.btnLogin.isEnabled = false
                    }
                    is AuthState.LoginSuccess -> {
                        binding.btnLogin.isEnabled = true
                        tokenManager.saveToken(state.token)

                        val intent = Intent(requireActivity(), GameActivity::class.java)
                        startActivity(intent)
                        requireActivity().finish()
                    }
                    is AuthState.Error -> {
                        binding.btnLogin.isEnabled = true
                        showError(state.message)
                        viewModel.resetState()
                    }
                    else -> { binding.btnLogin.isEnabled = true }
                }
            }
        }

        binding.btnLogin.setOnClickListener {
            val identifier = binding.tilUsernameEmail.editText?.text.toString().trim()
            val password = binding.tilPassword.editText?.text.toString()

            if (identifier.isBlank() || password.isBlank()) {
                showError(getString(R.string.all_fields_are_mandatory))
            } else {
                viewModel.login(LoginRequest(identifier, password))
            }
        }

        binding.tvGoToRegister.setOnClickListener {
            findNavController().navigate(R.id.action_login_to_register)
        }

        binding.tvContinueAsGuest.setOnClickListener {
            val intent = Intent(requireActivity(), rs.tim13.slagalica.core.ui.GameActivity::class.java)
            startActivity(intent)
            requireActivity().finish()
        }
    }
}