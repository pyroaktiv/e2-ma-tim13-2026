package rs.tim13.slagalica.korakpokorak.data

import rs.tim13.slagalica.korakpokorak.model.KorakPoKorakRound

interface KorakPoKorakGameRepository {
    /** Vraća runde „Korak po korak" (po jedna za svaki krug partije). */
    fun getRounds(): List<KorakPoKorakRound>
}
