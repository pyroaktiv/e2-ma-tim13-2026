package rs.tim13.slagalica.core.network.socket

/**
 * Sadržaj partije koji server šalje u poruci `match_found` — po jedna lista po igri.
 * Klijent ga mapira u domenske tipove kroz Remote*GameRepository implementacije.
 */
data class MatchContentDto(
    val koZnaZna: List<KoZnaZnaQuestionDto> = emptyList(),
    val spojnice: List<SpojniceRoundDto> = emptyList(),
    val asocijacije: List<AssociationsGameDto> = emptyList(),
    val skocko: List<List<String>> = emptyList(),       // imena simbola po tajnoj kombinaciji
    val mojBroj: List<MojBrojRoundDto> = emptyList(),
    val korakPoKorak: List<KorakPoKorakRoundDto> = emptyList()
)

data class KoZnaZnaQuestionDto(
    val text: String,
    val options: List<String>,
    val correctIndex: Int
)

data class SpojniceRoundDto(
    val leftItems: List<String>,
    val rightItems: List<String>,
    val solution: List<Int>     // solution[i] = indeks tačnog desnog pojma za levi i
)

data class AssociationsColumnDto(
    val index: Int,
    val fields: List<String>,
    val solution: String
)

data class AssociationsGameDto(
    val columns: List<AssociationsColumnDto>,
    val finalSolution: String
)

data class MojBrojRoundDto(
    val target: Int,
    val numbers: List<Int>
)

data class KorakPoKorakRoundDto(
    val clues: List<String>,
    val solution: String
)
