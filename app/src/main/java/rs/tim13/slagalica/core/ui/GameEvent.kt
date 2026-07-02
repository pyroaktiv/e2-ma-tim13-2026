package rs.tim13.slagalica.core.ui

import rs.tim13.slagalica.core.model.GameResult

/**
 * Događaji koje ViewModel igre emituje ka okruženju (fragmentu i budućem koordinatoru).
 *
 * - [MovePlayed]: potez koji treba poslati protivniku u multiplayer partiji.
 * - [GameFinished]: igra je gotova; nosi jedinstveni [GameResult] (skor + statistika 2.c).
 */
sealed class GameEvent {

    data class MovePlayed(val action: String, val payload: Map<String, Any>) : GameEvent()

    data class GameFinished(val result: GameResult) : GameEvent()
}
