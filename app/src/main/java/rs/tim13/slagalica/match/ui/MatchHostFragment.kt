package rs.tim13.slagalica.match.ui

import android.os.Bundle
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import rs.tim13.slagalica.R
import rs.tim13.slagalica.asocijacije.ui.AssociationsFragment
import rs.tim13.slagalica.core.model.Player
import rs.tim13.slagalica.core.network.socket.ChallengeResultEntryDto
import rs.tim13.slagalica.core.network.socket.MatchRewards
import rs.tim13.slagalica.core.network.socket.SocketManager
import rs.tim13.slagalica.core.ui.BaseFragment
import rs.tim13.slagalica.core.ui.GameCoordinator
import rs.tim13.slagalica.core.ui.GameCoordinatorHost
import rs.tim13.slagalica.databinding.FragmentMatchHostBinding
import rs.tim13.slagalica.turnir.ui.TurnirViewModel
import rs.tim13.slagalica.korakpokorak.ui.KorakPoKorakFragment
import rs.tim13.slagalica.koznazna.ui.KoZnaZnaFragment
import rs.tim13.slagalica.match.MatchGame
import rs.tim13.slagalica.match.MatchHost
import rs.tim13.slagalica.match.MatchMode
import rs.tim13.slagalica.match.MatchUiState
import rs.tim13.slagalica.match.MatchViewModel
import rs.tim13.slagalica.match.MatchViewModelFactory
import rs.tim13.slagalica.mojbroj.ui.MojBrojFragment
import rs.tim13.slagalica.skocko.ui.SkockoFragment
import rs.tim13.slagalica.spojnice.ui.SpojniceFragment

/**
 * Host partije: kreira [MatchViewModel], prebacuje dečje game-fragmente kroz fazu igranja i
 * prikazuje prekrivne ekrane (traženje protivnika, međurezultat, kraj). Most je za socket I/O.
 */
class MatchHostFragment : BaseFragment<FragmentMatchHostBinding>(FragmentMatchHostBinding::inflate),
    MatchHost, GameCoordinatorHost {

    private val mode: MatchMode by lazy {
        runCatching { MatchMode.valueOf(arguments?.getString(ARG_MODE) ?: "") }.getOrDefault(MatchMode.SOLO)
    }
    private val challengeId: String? by lazy { arguments?.getString(ARG_CHALLENGE_ID) }

    private val tvm: TurnirViewModel by activityViewModels()

    override val match: MatchViewModel by viewModels {
        val ctx = requireContext().applicationContext
        when (mode) {
            MatchMode.TOURNAMENT_SEMI -> tvm.foundMessage!!.let {
                MatchViewModelFactory(ctx, mode, tournamentMatchId = it.matchId, tournamentColor = it.color, tournamentContent = it.content)
            }
            MatchMode.TOURNAMENT_FINAL -> tvm.finalStartedMessage!!.let {
                MatchViewModelFactory(ctx, mode, tournamentMatchId = it.matchId, tournamentColor = it.color, tournamentContent = it.content)
            }
            else -> MatchViewModelFactory(ctx, mode, challengeId)
        }
    }
    override val gameCoordinator: GameCoordinator get() = match

    private var shownGame: MatchGame? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val needsSocket = mode == MatchMode.ONLINE || mode == MatchMode.CHALLENGE ||
                mode == MatchMode.TOURNAMENT_SEMI || mode == MatchMode.TOURNAMENT_FINAL
        if (needsSocket) {
            // Turnir: konekcija je već otvorena iz TurnirLobbyFragment-a; connect() je idempotentno.
            SocketManager.connect(requireContext())
            SocketManager.connected.observe(viewLifecycleOwner) { connected ->
                if (connected) match.onSocketConnected()
            }
            SocketManager.incoming.observe(viewLifecycleOwner) { message ->
                match.onServerMessage(message)
            }
        }

        match.uiState.observe(viewLifecycleOwner) { render(it) }

        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() = confirmLeave()
            }
        )

        binding.overlayAction.setOnClickListener { leaveAndExit() }
    }

    private fun render(state: MatchUiState) {
        when (state) {
            MatchUiState.Connecting -> showOverlay("Povezivanje...", "", progress = true)
            MatchUiState.Searching -> showOverlay("Tražim protivnika...", "Sačekajte da se neko pridruži", progress = true)
            is MatchUiState.PlayingGame -> {
                hideOverlay()
                showGameFragment(state.game)
            }
            is MatchUiState.InterGame -> showOverlay(
                title = scoreLine(state.blueTotal, state.redTotal, state.localColor),
                subtitle = state.next?.let { "Sledeća igra: ${it.displayName}" } ?: "Kraj partije...",
                progress = false
            )
            is MatchUiState.MatchOver -> renderMatchOver(state)
            is MatchUiState.Error -> showOverlay("Greška", state.message, progress = false, actionVisible = true)
            MatchUiState.ChallengeWaitingForOthers -> showOverlay(
                getString(R.string.challenge_match_waiting_title),
                getString(R.string.challenge_match_waiting_subtitle),
                progress = true
            )
            is MatchUiState.ChallengeFinished -> renderChallengeFinished(state)
            MatchUiState.TournamentWaiting -> showOverlay("Čekam turnirski rezultat...", "", progress = true)
            is MatchUiState.TournamentSemiResult -> renderTournamentSemi(state)
            is MatchUiState.TournamentFinalResult -> renderTournamentFinal(state)
        }
    }

    private fun renderTournamentSemi(state: MatchUiState.TournamentSemiResult) {
        val title = if (state.won) "Pobeda u polufinalu!" else "Poraz u polufinalu"
        val body = buildString {
            append("Rezultat: ${state.myScore} : ${state.opponentScore}\n")
            state.rewards?.let { append("Zvezde: ${signed(it.starsDelta)}   Tokeni: ${signed(it.tokensDelta)}") }
                ?: append("Bez nagrada.")
        }
        showOverlay(title, body, progress = false, actionVisible = true)
    }

    private fun renderTournamentFinal(state: MatchUiState.TournamentFinalResult) {
        val title = if (state.amIWinner) "Pobeda u finalu!" else "Poraz u finalu"
        val r = state.myRewards
        val body = buildString {
            append("Rezultat: ${state.myScore} : ${state.opponentScore}\n")
            append("Zvezde: ${signed(r.starsDelta)}   Tokeni: ${signed(r.tokensDelta)}\n")
            append("Ukupno: ${r.totalStars}★ • ${r.tokens} tokena • liga ${r.league}")
        }
        showOverlay(title, body, progress = false, actionVisible = true)
    }

    private fun renderChallengeFinished(state: MatchUiState.ChallengeFinished) {
        val ranked = state.results.sortedByDescending { it.score }
        val sb = StringBuilder()
        ranked.forEachIndexed { index, r -> sb.append(resultRow(index + 1, r)).append('\n') }
        showOverlay(getString(R.string.challenge_result_title), sb.toString().trim(), progress = false, actionVisible = true)
    }

    private fun resultRow(rank: Int, r: ChallengeResultEntryDto): String = getString(
        R.string.challenge_result_row, rank, r.username, r.score, signed(r.rewardStars), signed(r.rewardTokens)
    )

    private fun renderMatchOver(state: MatchUiState.MatchOver) {
        val (mine, theirs) = scoreFor(state.blueTotal, state.redTotal, state.localColor)
        val title = when {
            mine > theirs -> "Pobeda!"
            mine < theirs -> "Poraz"
            else -> "Nerešeno"
        }
        val sb = StringBuilder("Rezultat: $mine : $theirs\n")
        if (state.opponentLeft) sb.append("Protivnik je napustio partiju.\n")
        state.rewards?.let { sb.append(rewardsText(it)) }
            ?: if (match.isOnline) sb.append("Obračun nagrada...") else Unit
        showOverlay(title, sb.toString().trim(), progress = false, actionVisible = true)
    }

    private fun rewardsText(r: MatchRewards): String = buildString {
        append("Zvezde: ${signed(r.starsDelta)}   Tokeni: ${signed(r.tokensDelta)}\n")
        append("Ukupno: ${r.totalStars}★ • ${r.tokens} tokena • liga ${r.league}")
    }

    private fun showGameFragment(game: MatchGame) {
        if (shownGame == game) return
        shownGame = game
        childFragmentManager.beginTransaction()
            .replace(binding.gameContainer.id, fragmentFor(game))
            .commit()
    }

    private fun fragmentFor(game: MatchGame): Fragment = when (game) {
        MatchGame.KO_ZNA_ZNA -> KoZnaZnaFragment()
        MatchGame.SPOJNICE -> SpojniceFragment()
        MatchGame.ASOCIJACIJE -> AssociationsFragment()
        MatchGame.SKOCKO -> SkockoFragment()
        MatchGame.KORAK_PO_KORAK -> KorakPoKorakFragment()
        MatchGame.MOJ_BROJ -> MojBrojFragment()
    }

    private fun showOverlay(title: String, subtitle: String, progress: Boolean, actionVisible: Boolean = false) {
        binding.overlay.visibility = View.VISIBLE
        binding.overlayTitle.text = title
        binding.overlaySubtitle.text = subtitle
        binding.overlayProgress.visibility = if (progress) View.VISIBLE else View.GONE
        binding.overlayAction.visibility = if (actionVisible) View.VISIBLE else View.GONE
    }

    private fun hideOverlay() {
        binding.overlay.visibility = View.GONE
    }

    private fun confirmLeave() {
        val state = match.uiState.value
        if (state is MatchUiState.MatchOver || state is MatchUiState.Error ||
            state is MatchUiState.ChallengeFinished || state == MatchUiState.ChallengeWaitingForOthers ||
            state is MatchUiState.TournamentSemiResult || state is MatchUiState.TournamentFinalResult ||
            state == MatchUiState.TournamentWaiting
        ) {
            leaveAndExit()
            return
        }
        AlertDialog.Builder(requireContext())
            .setTitle("Napustiti partiju?")
            .setMessage("Napuštanjem gubite partiju.")
            .setPositiveButton("Napusti") { _, _ -> leaveAndExit() }
            .setNegativeButton("Otkaži", null)
            .show()
    }

    private fun leaveAndExit() {
        match.leaveMatch()
        when (mode) {
            MatchMode.ONLINE -> {
                SocketManager.disconnect()
                findNavController().popBackStack()
            }
            MatchMode.CHALLENGE -> {
                SocketManager.disconnect()
                findNavController().popBackStack(R.id.homeFragment, false)
            }
            MatchMode.TOURNAMENT_SEMI, MatchMode.TOURNAMENT_FINAL -> {
                // Ne prekidamo socket — turnir još traje za ostale igrače.
                findNavController().popBackStack(R.id.homeFragment, false)
            }
            else -> findNavController().popBackStack()
        }
    }

    private fun scoreFor(blue: Int, red: Int, local: Player): Pair<Int, Int> =
        if (local == Player.BLUE) blue to red else red to blue

    private fun scoreLine(blue: Int, red: Int, local: Player): String {
        val (mine, theirs) = scoreFor(blue, red, local)
        return "Ti  $mine : $theirs  Protivnik"
    }

    private fun signed(value: Int): String = if (value >= 0) "+$value" else "$value"

    companion object {
        const val ARG_MODE = "mode"
        const val ARG_CHALLENGE_ID = "challengeId"
    }
}
