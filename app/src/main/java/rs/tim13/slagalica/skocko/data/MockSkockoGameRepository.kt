package rs.tim13.slagalica.skocko.data

import rs.tim13.slagalica.skocko.model.SkockoSymbol

class MockSkockoGameRepository : SkockoGameRepository {
    override fun getSecrets(): List<List<SkockoSymbol>> = List(2) {
        List(4) { SkockoSymbol.entries.random() }
    }
}
