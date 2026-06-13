package rs.tim13.slagalica.mojbroj.model

import rs.tim13.slagalica.core.model.BaseGame
import rs.tim13.slagalica.core.model.Player

/**
 * Logika jedne runde „Moj broj". Oba igrača rešavaju istu zagonetku ([target] + [numbers]);
 * svaki prijavi svoj rezultat (ili null ako ništa ne unese), a bodovanje vrši
 * [MojBrojScoringEngine]. [roundLeader] je igrač čija je runda (bitno za nerešen ishod).
 */
class MojBrojGame(
    val target: Int,
    val numbers: List<Int>,
    val roundLeader: Player,
    isSinglePlayer: Boolean = false,
    initialOpponentDisconnected: Boolean = false
) : BaseGame(isSinglePlayer, initialOpponentDisconnected) {

    init {
        require(numbers.size == NUMBER_COUNT)
    }

    private val results = mutableMapOf<Player, Int?>()
    private val submitted = mutableSetOf<Player>()

    fun submitResult(player: Player, value: Int?) {
        if (player in submitted) return
        submitted.add(player)
        results[player] = value
    }

    fun hasSubmitted(player: Player): Boolean = player in submitted
    val bothSubmitted: Boolean get() = submitted.containsAll(listOf(Player.BLUE, Player.RED))
    fun resultOf(player: Player): Int? = results[player]
    fun foundTarget(player: Player): Boolean = results[player] == target

    override fun calculateScore(): Map<Player, Int> =
        MojBrojScoringEngine.roundScore(target, results[Player.BLUE], results[Player.RED], roundLeader)

    companion object {
        const val NUMBER_COUNT = 6
    }
}
