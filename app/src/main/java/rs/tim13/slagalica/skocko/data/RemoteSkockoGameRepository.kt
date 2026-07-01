package rs.tim13.slagalica.skocko.data

import rs.tim13.slagalica.skocko.model.SkockoSymbol

/** Repozitorijum Skočka čije tajne kombinacije dolaze sa servera (imena simbola). */
class RemoteSkockoGameRepository(
    private val secrets: List<List<String>>
) : SkockoGameRepository {
    override fun getSecrets(): List<List<SkockoSymbol>> =
        secrets.map { secret -> secret.map { SkockoSymbol.valueOf(it) } }
}
