package rs.tim13.slagalica.match

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import rs.tim13.slagalica.asocijacije.data.RemoteAssociationsGameRepository
import rs.tim13.slagalica.asocijacije.data.MockAssociationsGameRepository
import rs.tim13.slagalica.asocijacije.model.AssociationsGameRepository
import rs.tim13.slagalica.core.model.GameConfig
import rs.tim13.slagalica.core.model.GameResult
import rs.tim13.slagalica.core.model.Player
import rs.tim13.slagalica.core.network.socket.ClientMessage
import rs.tim13.slagalica.core.network.socket.MatchContentDto
import rs.tim13.slagalica.core.network.socket.PerGameStatsDto
import rs.tim13.slagalica.core.network.socket.ServerMessage
import rs.tim13.slagalica.core.network.socket.SocketManager
import rs.tim13.slagalica.core.ui.GameCoordinator
import rs.tim13.slagalica.core.ui.RemoteGame
import rs.tim13.slagalica.koznazna.data.KoZnaZnaGameRepository
import rs.tim13.slagalica.koznazna.data.MockKoZnaZnaGameRepository
import rs.tim13.slagalica.koznazna.data.RemoteKoZnaZnaGameRepository
import rs.tim13.slagalica.korakpokorak.data.KorakPoKorakGameRepository
import rs.tim13.slagalica.korakpokorak.data.MockKorakPoKorakGameRepository
import rs.tim13.slagalica.korakpokorak.data.RemoteKorakPoKorakGameRepository
import rs.tim13.slagalica.mojbroj.data.MockMojBrojGameRepository
import rs.tim13.slagalica.mojbroj.data.MojBrojGameRepository
import rs.tim13.slagalica.mojbroj.data.RemoteMojBrojGameRepository
import rs.tim13.slagalica.skocko.data.MockSkockoGameRepository
import rs.tim13.slagalica.skocko.data.RemoteSkockoGameRepository
import rs.tim13.slagalica.skocko.data.SkockoGameRepository
import rs.tim13.slagalica.spojnice.data.MockSpojniceGameRepository
import rs.tim13.slagalica.spojnice.data.RemoteSpojniceGameRepository
import rs.tim13.slagalica.spojnice.data.SpojniceGameRepository

/**
 * Orkestrator partije (spec 3, „Igranje partija"). Vodi svih šest igara redom, sabira skorove,
 * a između igara pravi kratku pauzu sa tabelom rezultata. Implementira [GameCoordinator] —
 * pojedinačne igre ne znaju ništa o partiji, samo mu prijavljuju poteze i kraj.
 *
 * SOLO: igre se igraju lokalno (Mock sadržaj, single-player varijante), bez servera i nagrada.
 * ONLINE: server šalje sadržaj i protivnika; potezi idu preko [SocketManager]; nagrade obračunava
 * server po prijavi rezultata.
 */
class MatchViewModel(private val mode: MatchMode) : ViewModel(), GameCoordinator {

    private val _uiState = MutableLiveData<MatchUiState>()
    val uiState: LiveData<MatchUiState> = _uiState

    private val order = MatchGame.entries
    private var currentIndex = 0
    private var blueTotal = 0
    private var redTotal = 0
    private val perGame = mutableListOf<PerGameStatsDto>()
    private val finishedIndices = mutableSetOf<Int>()

    private var matchId: String = ""
    private var localColor: Player = Player.BLUE
    private var content: MatchContentDto? = null
    private var attachedGame: RemoteGame? = null
    private var opponentLeft = false
    private var findSent = false

    val isOnline: Boolean get() = mode == MatchMode.ONLINE

    init {
        when (mode) {
            MatchMode.SOLO -> showGame(0)
            MatchMode.ONLINE -> _uiState.value = MatchUiState.Connecting
        }
    }

    // region Socket lifecycle (poziva host)

    fun onSocketConnected() {
        if (mode == MatchMode.ONLINE && !findSent) {
            findSent = true
            SocketManager.send(ClientMessage.FindMatch())
            _uiState.value = MatchUiState.Searching
        }
    }

    fun onServerMessage(message: ServerMessage) {
        when (message) {
            ServerMessage.Idle -> Unit
            is ServerMessage.MatchFound -> {
                if (matchId.isNotEmpty()) return // već smo u partiji (ignoriši ponovljenu poruku)
                matchId = message.matchId
                localColor = runCatching { Player.valueOf(message.color) }.getOrDefault(Player.BLUE)
                content = message.content
                currentIndex = 0
                showGame(0)
            }
            is ServerMessage.RemoteMove -> {
                if (matchId.isNotEmpty() && message.gameIndex == currentIndex) {
                    attachedGame?.onRemoteMove(message.action, message.payload)
                }
            }
            is ServerMessage.OpponentLeft -> {
                if (matchId.isEmpty()) return
                opponentLeft = true
                attachedGame?.handleOpponentDisconnected()
            }
            is ServerMessage.MatchOver -> {
                if (matchId.isEmpty()) return
                _uiState.value = MatchUiState.MatchOver(blueTotal, redTotal, localColor, message.rewards, opponentLeft)
            }
            is ServerMessage.ServerError -> {
                _uiState.value = MatchUiState.Error(message.message)
            }
        }
    }

    /** Igrač napušta partiju (forfeit). */
    fun leaveMatch() {
        if (mode == MatchMode.ONLINE && matchId.isNotEmpty()) {
            SocketManager.send(ClientMessage.LeaveMatch(matchId = matchId))
        }
    }

    // endregion

    // region GameCoordinator

    override val gameConfig: GameConfig
        get() = GameConfig(
            localPlayer = localColor,
            isSinglePlayer = mode == MatchMode.SOLO,
            initialOpponentDisconnected = opponentLeft
        )

    override fun attachGame(game: RemoteGame) {
        attachedGame = game
    }

    override fun onLocalMove(action: String, payload: Map<String, Any>) {
        if (mode == MatchMode.ONLINE) {
            SocketManager.send(
                ClientMessage.MatchMove(matchId = matchId, gameIndex = currentIndex, action = action, payload = payload)
            )
        }
    }

    override fun onGameFinished(result: GameResult) {
        if (!finishedIndices.add(currentIndex)) return // zaštita od ponovljene isporuke event-a

        blueTotal += result.blueScore
        redTotal += result.redScore
        perGame.add(PerGameStatsDto(order[currentIndex].key, result.statistics))

        val finished = order[currentIndex]
        _uiState.value = MatchUiState.InterGame(finished, finished.next(), blueTotal, redTotal, localColor)

        viewModelScope.launch {
            delay(INTER_GAME_MS)
            advance()
        }
    }

    // endregion

    // region Repozitorijumi po igri (Remote za online, Mock za solo)

    fun koZnaZnaRepository(): KoZnaZnaGameRepository =
        content?.let { RemoteKoZnaZnaGameRepository(it.koZnaZna) } ?: MockKoZnaZnaGameRepository()

    fun spojniceRepository(): SpojniceGameRepository =
        content?.let { RemoteSpojniceGameRepository(it.spojnice) } ?: MockSpojniceGameRepository()

    fun asocijacijeRepository(): AssociationsGameRepository =
        content?.let { RemoteAssociationsGameRepository(it.asocijacije) } ?: MockAssociationsGameRepository()

    fun skockoRepository(): SkockoGameRepository =
        content?.let { RemoteSkockoGameRepository(it.skocko) } ?: MockSkockoGameRepository()

    fun mojBrojRepository(): MojBrojGameRepository =
        content?.let { RemoteMojBrojGameRepository(it.mojBroj) } ?: MockMojBrojGameRepository()

    fun korakPoKorakRepository(): KorakPoKorakGameRepository =
        content?.let { RemoteKorakPoKorakGameRepository(it.korakPoKorak) } ?: MockKorakPoKorakGameRepository()

    // endregion

    private fun advance() {
        currentIndex++
        if (currentIndex < order.size) {
            showGame(currentIndex)
        } else {
            finishMatch()
        }
    }

    private fun showGame(index: Int) {
        attachedGame = null
        _uiState.value = MatchUiState.PlayingGame(order[index], blueTotal, redTotal, localColor)
    }

    private fun finishMatch() {
        if (mode == MatchMode.ONLINE) {
            SocketManager.send(
                ClientMessage.ReportResult(
                    matchId = matchId,
                    blueScore = blueTotal,
                    redScore = redTotal,
                    perGame = perGame.toList()
                )
            )
            // Čekamo `match_over` sa nagradama; do tada prikaži kraj bez nagrada.
            _uiState.value = MatchUiState.MatchOver(blueTotal, redTotal, localColor, rewards = null, opponentLeft = opponentLeft)
        } else {
            _uiState.value = MatchUiState.MatchOver(blueTotal, redTotal, localColor, rewards = null, opponentLeft = false)
        }
    }

    companion object {
        const val INTER_GAME_MS = 3000L
    }
}
