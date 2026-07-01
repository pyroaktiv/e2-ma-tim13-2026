package rs.tim13.slagalica.koznazna.model

/**
 * Jedno pitanje opšteg znanja: tekst, četiri ponuđena odgovora i indeks tačnog.
 */
data class KoZnaZnaQuestion(
    val text: String,
    val options: List<String>,
    val correctIndex: Int
) {
    init {
        require(options.size == OPTION_COUNT) { "Pitanje mora imati $OPTION_COUNT odgovora" }
        require(correctIndex in 0 until OPTION_COUNT)
    }

    companion object {
        const val OPTION_COUNT = 4
    }
}

/**
 * Odgovor jednog igrača na pitanje. [elapsedMs] je vreme (ms) od prikaza pitanja do odgovora,
 * mereno na igračevom telefonu i preneseno protivniku — tako oba telefona porede iste vrednosti
 * i deterministički određuju bržeg igrača (pravilo „bodove dobija brži" kad su oba tačna).
 */
data class KoZnaZnaAnswer(
    val optionIndex: Int,
    val elapsedMs: Long
)
