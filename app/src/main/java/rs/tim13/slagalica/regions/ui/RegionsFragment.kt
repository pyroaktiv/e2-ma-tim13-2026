package rs.tim13.slagalica.regions.ui

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.overlay.Marker
import rs.tim13.slagalica.R
import rs.tim13.slagalica.core.network.RetrofitClient
import rs.tim13.slagalica.core.ui.BaseFragment
import rs.tim13.slagalica.core.util.ProfileResources
import rs.tim13.slagalica.databinding.FragmentRegionsBinding
import rs.tim13.slagalica.regions.data.api.RegionApiService
import rs.tim13.slagalica.regions.data.api.dto.RegionDto
import rs.tim13.slagalica.regions.data.api.dto.RegionStatsDto

/**
 * Prikaz regiona (spec 5): mapa Srbije sa tačkom svakog igrača, mesečna rang lista regiona
 * (sa istaknutim regionom igrača) i statistika regiona na klik. Gađa backend rute Studenta 3.
 */
class RegionsFragment : BaseFragment<FragmentRegionsBinding>(FragmentRegionsBinding::inflate) {

    private lateinit var adapter: RegionRankingAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // osmdroid mora biti konfigurisan pre nego što se MapView napravi (pri inflate-u).
        val ctx = requireContext().applicationContext
        Configuration.getInstance().load(ctx, ctx.getSharedPreferences("osmdroid", Context.MODE_PRIVATE))
        Configuration.getInstance().userAgentValue = ctx.packageName
    }

    private fun api(): RegionApiService =
        RetrofitClient.getClient(requireContext()).create(RegionApiService::class.java)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.map.setTileSource(TileSourceFactory.MAPNIK)
        binding.map.setMultiTouchControls(true)
        binding.map.controller.setZoom(7.0)
        binding.map.controller.setCenter(GeoPoint(44.0, 20.9)) // centar Srbije
        binding.tvCycleRange.text = ""

        adapter = RegionRankingAdapter(onClick = ::showRegionStats)
        binding.rvRegions.layoutManager = LinearLayoutManager(requireContext())
        binding.rvRegions.adapter = adapter

        loadRanking()
        loadMap()
    }

    private fun loadRanking() {
        viewLifecycleOwner.lifecycleScope.launch {
            val regions = runCatching {
                val r = api().getRegions()
                if (r.isSuccessful) r.body() else null
            }.getOrNull().orEmpty()
            adapter.submitList(regions)
        }
    }

    private fun loadMap() {
        viewLifecycleOwner.lifecycleScope.launch {
            val points = runCatching {
                val r = api().getMap()
                if (r.isSuccessful) r.body() else null
            }.getOrNull().orEmpty()

            val ctx = requireContext()
            points.forEach { p ->
                val pin = ContextCompat.getDrawable(ctx, R.drawable.ic_region_pin)?.mutate()?.apply {
                    setTint(ProfileResources.regionColor(p.region))
                }
                val marker = Marker(binding.map).apply {
                    position = GeoPoint(p.lat, p.lng)
                    title = p.region
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                    icon = pin
                }
                binding.map.overlays.add(marker)
            }
            binding.map.invalidate()
        }
    }

    private fun showRegionStats(region: RegionDto) {
        viewLifecycleOwner.lifecycleScope.launch {
            val stats = runCatching {
                val r = api().getStats(region.name)
                if (r.isSuccessful) r.body() else null
            }.getOrNull() ?: run {
                showError(getString(R.string.regions_stats_error))
                return@launch
            }
            MaterialAlertDialogBuilder(requireContext())
                .setTitle(region.name)
                .setMessage(statsText(stats))
                .setPositiveButton(android.R.string.ok, null)
                .show()
        }
    }

    private fun statsText(s: RegionStatsDto): String = buildString {
        appendLine(getString(R.string.regions_stat_first, s.firstPlaceCount))
        appendLine(getString(R.string.regions_stat_second, s.secondPlaceCount))
        appendLine(getString(R.string.regions_stat_third, s.thirdPlaceCount))
        appendLine(getString(R.string.regions_stat_active, s.activePlayers))
        append(getString(R.string.regions_stat_registered, s.totalPlayers))
    }

    override fun onResume() {
        super.onResume()
        binding.map.onResume()
    }

    override fun onPause() {
        binding.map.onPause()
        super.onPause()
    }
}
