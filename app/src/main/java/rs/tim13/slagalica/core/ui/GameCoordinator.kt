package rs.tim13.slagalica.core.ui

import rs.tim13.slagalica.core.model.GameConfig
import rs.tim13.slagalica.core.model.GameResult

/**
 * Ono što orkestrator partije sme da uradi nad pojedinačnom igrom: da joj dostavi protivnikov
 * potez i da je obavesti da je protivnik napustio partiju. [BaseGameViewModel] ovo implementira.
 */
interface RemoteGame {
    fun onRemoteMove(action: String, payload: Map<String, Any>)
    fun handleOpponentDisconnected()
}

/**
 * Most između pojedinačne igre i orkestratora partije ([rs.tim13.slagalica.match.MatchViewModel]).
 *
 * Igra ne poznaje partiju — samo, preko ovog interfejsa, prijavljuje svoje poteze i kraj, a
 * orkestrator joj (kroz [attachGame]) prosleđuje protivnikove poteze. U solo režimu potezi se
 * ignorišu; u online režimu idu preko socketa.
 */
interface GameCoordinator {
    val gameConfig: GameConfig
    fun attachGame(game: RemoteGame)
    fun onLocalMove(action: String, payload: Map<String, Any>)
    fun onGameFinished(result: GameResult)
}

/**
 * Host (npr. MatchHostFragment) koji izlaže [GameCoordinator] svojim dečjim game-fragmentima.
 */
interface GameCoordinatorHost {
    val gameCoordinator: GameCoordinator
}
