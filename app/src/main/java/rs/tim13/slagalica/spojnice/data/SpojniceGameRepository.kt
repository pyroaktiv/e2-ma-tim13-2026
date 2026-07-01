package rs.tim13.slagalica.spojnice.data

import rs.tim13.slagalica.spojnice.model.SpojniceRound

interface SpojniceGameRepository {
    /** Vraća runde Spojnica (po jedna za svaki krug partije). */
    fun getRounds(): List<SpojniceRound>
}
