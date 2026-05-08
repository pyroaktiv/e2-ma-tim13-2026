package rs.tim13.slagalica.skocko.data

import rs.tim13.slagalica.skocko.model.SkockoSymbol

interface SkockoGameRepository {
    fun getSecrets(): List<List<SkockoSymbol>>
}
