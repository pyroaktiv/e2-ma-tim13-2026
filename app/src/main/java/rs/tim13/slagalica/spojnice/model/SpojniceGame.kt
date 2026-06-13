package rs.tim13.slagalica.spojnice.model

import rs.tim13.slagalica.core.model.Player
import rs.tim13.slagalica.core.model.TurnBasedGame

/**
 * Logika jedne runde Spojnica (spec 2).
 *
 * Vodeći igrač ([leadPlayer]) ima [PAIR_COUNT] pokušaja da poveže parove. Kada potroši
 * pokušaje (ili istekne vreme), protivnik u fazi popravka ([isRecoveryPhase]) dobija onoliko
 * pokušaja koliko je parova ostalo. Svaki tačno povezan par nosi [POINTS_PER_PAIR] boda
 * igraču koji ga je povezao.
 */
class SpojniceGame(
    val leftItems: List<String>,
    val rightItems: List<String>,
    private val solution: Map<Int, Int>,
    initialPlayer: Player,
    isSinglePlayer: Boolean,
    initialOpponentDisconnected: Boolean = false
) : TurnBasedGame(initialPlayer, isSinglePlayer, initialOpponentDisconnected) {

    init {
        require(leftItems.size == PAIR_COUNT)
        require(rightItems.size == PAIR_COUNT)
        require(solution.size == PAIR_COUNT)
    }

    val leadPlayer: Player = initialPlayer

    private val connectedBy = arrayOfNulls<Player>(PAIR_COUNT)
    private val scores = mutableMapOf(Player.BLUE to 0, Player.RED to 0)
    private val attempts = mutableMapOf(Player.BLUE to 0, Player.RED to 0)

    var isRecoveryPhase = false
        private set
    private var leadAttempts = 0
    private var recoveryAttempts = 0
    private var recoveryAllowed = 0

    val solvedCount: Int get() = connectedBy.count { it != null }
    val allSolved: Boolean get() = solvedCount == PAIR_COUNT

    fun isLeftConnected(leftIndex: Int): Boolean = connectedBy[leftIndex] != null

    /** Indeks desnog pojma povezan sa datim levim (ili null ako levi nije povezan). */
    fun connectedRightIndex(leftIndex: Int): Int? =
        if (isLeftConnected(leftIndex)) solution[leftIndex] else null

    /** Snapshot: ko je povezao svaki levi pojam (null = nije povezan). */
    fun connectionsByLeft(): List<Player?> = connectedBy.toList()

    /** Indeksi desnih pojmova koji su već zauzeti tačnim spajanjem. */
    fun connectedRightIndices(): Set<Int> =
        connectedBy.indices.filter { connectedBy[it] != null }.map { solution.getValue(it) }.toSet()

    fun connectedCountFor(player: Player): Int = connectedBy.count { it == player }
    fun attemptsFor(player: Player): Int = attempts.getValue(player)

    val isLeadPhaseExhausted: Boolean get() = allSolved || leadAttempts >= PAIR_COUNT
    val isRecoveryPhaseExhausted: Boolean get() = allSolved || recoveryAttempts >= recoveryAllowed

    /**
     * Pokušaj spajanja levog i desnog pojma od strane [activePlayer].
     * Vraća true ako je par tačan. Pogrešan pokušaj se broji ali ne menja stanje table.
     */
    fun attemptConnection(leftIndex: Int, rightIndex: Int): Boolean {
        if (isLeftConnected(leftIndex)) return false

        attempts[activePlayer] = attempts.getValue(activePlayer) + 1
        if (isRecoveryPhase) recoveryAttempts++ else leadAttempts++

        val correct = solution[leftIndex] == rightIndex
        if (correct) {
            connectedBy[leftIndex] = activePlayer
            scores[activePlayer] = scores.getValue(activePlayer) + POINTS_PER_PAIR
        }
        return correct
    }

    /** Ima li smisla dati protivniku šansu za popravak (ima nepovezanih i protivnik je prisutan). */
    fun canStartRecovery(): Boolean = !isSinglePlayer && !isOpponentDisconnected && !allSolved

    fun beginRecovery() {
        isRecoveryPhase = true
        recoveryAllowed = PAIR_COUNT - solvedCount
        recoveryAttempts = 0
        if (activePlayer == leadPlayer) switchPlayer()
    }

    override fun calculateScore(): Map<Player, Int> = scores.toMap()

    companion object {
        const val PAIR_COUNT = 5
        const val POINTS_PER_PAIR = 2
    }
}
