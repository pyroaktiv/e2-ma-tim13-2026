package rs.tim13.slagalica.koznazna.data

import rs.tim13.slagalica.core.network.socket.KoZnaZnaQuestionDto
import rs.tim13.slagalica.koznazna.model.KoZnaZnaQuestion

/** Repozitorijum „Ko zna zna" čiji sadržaj dolazi sa servera (ista partija za oba igrača). */
class RemoteKoZnaZnaGameRepository(
    private val questions: List<KoZnaZnaQuestionDto>
) : KoZnaZnaGameRepository {
    override fun getQuestions(): List<KoZnaZnaQuestion> =
        questions.map { KoZnaZnaQuestion(it.text, it.options, it.correctIndex) }
}
