package rs.tim13.slagalica.koznazna.data

import rs.tim13.slagalica.koznazna.model.KoZnaZnaQuestion

interface KoZnaZnaGameRepository {
    /** Vraća pet pitanja za jednu partiju „Ko zna zna". */
    fun getQuestions(): List<KoZnaZnaQuestion>
}
