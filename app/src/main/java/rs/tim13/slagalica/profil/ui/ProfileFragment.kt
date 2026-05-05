package rs.tim13.slagalica.profil.ui

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.navigation.fragment.findNavController
import rs.tim13.slagalica.R
import rs.tim13.slagalica.core.ui.BaseFragment
import rs.tim13.slagalica.databinding.FragmentProfilBinding

class ProfilFragment : BaseFragment<FragmentProfilBinding>(FragmentProfilBinding::inflate) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupUI()
    }

    private fun setupUI() {
        binding.fabEditAvatar.setOnClickListener {
            Toast.makeText(requireContext(), "Otvara se galerija za izmenu avatara", Toast.LENGTH_SHORT).show()
        }

        binding.btnLogout.setOnClickListener {
            Toast.makeText(requireContext(), "Korisnik odjavljen", Toast.LENGTH_SHORT).show()
        }
    }
}