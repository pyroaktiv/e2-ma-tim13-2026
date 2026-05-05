package rs.tim13.slagalica.core.ui

import android.os.Bundle
import android.view.View
import androidx.navigation.fragment.findNavController
import rs.tim13.slagalica.R
import rs.tim13.slagalica.databinding.FragmentDemoGameListBinding

class DemoGameListFragment : BaseFragment<FragmentDemoGameListBinding>(FragmentDemoGameListBinding::inflate) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnViewProfile.setOnClickListener {
            findNavController().navigate(R.id.action_list_to_profil)
        }

        binding.btnPlayMojBroj.setOnClickListener {
            findNavController().navigate(R.id.action_list_to_mojBroj)
        }

        binding.btnPlayKorakPoKorak.setOnClickListener {
            findNavController().navigate(R.id.action_list_to_korakPoKorak)
        }

        binding.btnPlayKoZnaZna.setOnClickListener {
            findNavController().navigate(R.id.action_list_to_koZnaZna)
        }

        binding.btnPlaySpojnice.setOnClickListener {
            findNavController().navigate(R.id.action_list_to_spojnice)
        }
    }
}