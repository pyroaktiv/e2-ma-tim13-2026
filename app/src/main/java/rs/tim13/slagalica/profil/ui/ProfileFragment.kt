package rs.tim13.slagalica.profil.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import rs.tim13.slagalica.core.ui.BaseFragment
import rs.tim13.slagalica.databinding.FragmentProfilBinding
// Obavezno dodajemo import za AuthActivity
import rs.tim13.slagalica.auth.ui.AuthActivity

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
            
            val intent = Intent(requireContext(), AuthActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            startActivity(intent)
        }
    }
}