package rs.tim13.slagalica.auth.ui

import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import androidx.navigation.fragment.findNavController
import rs.tim13.slagalica.R
import rs.tim13.slagalica.core.ui.BaseFragment
import rs.tim13.slagalica.databinding.FragmentAuthRegisterBinding

class RegisterFragment : BaseFragment<FragmentAuthRegisterBinding>(FragmentAuthRegisterBinding::inflate) {

    private val regions = listOf(
        "Vojvodina",
        "Beograd",
        "Šumadija i Zapadna Srbija",
        "Južna i Istočna Srbija",
    )

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, regions)
        binding.actvRegion.setAdapter(adapter)

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
            findNavController().navigate(R.id.action_register_to_login)
        }
    }
}