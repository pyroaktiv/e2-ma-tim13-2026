package rs.tim13.slagalica.koznazna.model

import rs.tim13.slagalica.core.model.BaseGame
import rs.tim13.slagalica.core.model.Player

/**
 * Logika igre „Ko zna zna": niz pitanja koja oba igrača rešavaju istovremeno.
 *
 * Igra nije turn-based — oba igrača mogu da odgovore na isto pitanje, a redosled
 * odgovora ([KoZnaZnaAnswer.order]) odlučuje ko je brži kada su oba tačna. Bodovi i
 * statistika se akumuliraju kroz [calculateScore], [blueCorrect] itd.
 */
class KoZnaZnaGame(
    val questions: List<KoZnaZnaQuestion>,
    isSinglePlayer: Boolean = false,
    initialOpponentDisconnected: Boolean = false
) : BaseGame(isSinglePlayer, initialOpponentDisconnected) {

    init {
        require(questions.isNotEmpty())
    }

    var currentIndex = 0
        private set

    val currentQuestion: KoZnaZnaQuestion get() = questions[currentIndex]
    val correctIndex: Int get() = currentQuestion.correctIndex
    val questionCount: Int get() = questions.size
    val isLastQuestion: Boolean get() = currentIndex == questions.lastIndex

    var isCurrentResolved = false
        private set

    private val answers = mutableMapOf<Player, KoZnaZnaAnswer>()
    private var answerOrder = 0

    private val totals = mutableMapOf(Player.BLUE to 0, Player.RED to 0)

    var blueCorrect = 0; private set
    var blueWrong = 0; private set
    var redCorrect = 0; private set
    var redWrong = 0; private set

    fun answerOf(player: Player): KoZnaZnaAnswer? = answers[player]
    fun hasAnswered(player: Player): Boolean = answers.containsKey(player)
    val bothAnswered: Boolean get() = hasAnswered(Player.BLUE) && hasAnswered(Player.RED)

    /** Beleži odgovor igrača za tekuće pitanje (najviše jednom po pitanju). */
    fun submitAnswer(player: Player, optionIndex: Int): Boolean {
        if (isCurrentResolved || hasAnswered(player)) return false
        require(optionIndex in 0 until KoZnaZnaQuestion.OPTION_COUNT)
        answers[player] = KoZnaZnaAnswer(optionIndex, answerOrder++)
        return true
    }

    /** Zaključuje tekuće pitanje: obračun poena i statistike. Idempotentno. */
    fun resolveCurrentQuestion() {
        if (isCurrentResolved) return
        val delta = KoZnaZnaScoringEngine.questionScore(correctIndex, answers[Player.BLUE], answers[Player.RED])
        totals[Player.BLUE] = totals.getValue(Player.BLUE) + delta.getValue(Player.BLUE)
        totals[Player.RED] = totals.getValue(Player.RED) + delta.getValue(Player.RED)
        recordStat(Player.BLUE, answers[Player.BLUE])
        recordStat(Player.RED, answers[Player.RED])
        isCurrentResolved = true
    }

    private fun recordStat(player: Player, answer: KoZnaZnaAnswer?) {
        if (answer == null) return
        val correct = answer.optionIndex == correctIndex
        when (player) {
            Player.BLUE -> if (correct) blueCorrect++ else blueWrong++
            Player.RED -> if (correct) redCorrect++ else redWrong++
        }
    }

    /** Prelazi na sledeće pitanje. Vraća false ako nema više pitanja. */
    fun advance(): Boolean {
        if (!isCurrentResolved || isLastQuestion) return false
        currentIndex++
        answers.clear()
        isCurrentResolved = false
        return true
    }

    override fun calculateScore(): Map<Player, Int> = totals.toMap()
}
