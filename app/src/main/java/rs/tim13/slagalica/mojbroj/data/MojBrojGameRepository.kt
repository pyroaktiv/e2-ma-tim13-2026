package rs.tim13.slagalica.mojbroj.data

import rs.tim13.slagalica.mojbroj.model.MojBrojRound

interface MojBrojGameRepository {
    /** Vraća zagonetke (traženi broj + šest brojeva) za runde partije. */
    fun getRounds(): List<MojBrojRound>
}
