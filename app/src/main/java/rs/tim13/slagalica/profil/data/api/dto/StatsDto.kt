package rs.tim13.slagalica.profil.data.api.dto

/**
 * Statistika igrača sa `/api/user/stats` (spec 2.c). Gson koristi LOWER_CASE_WITH_UNDERSCORES,
 * pa se npr. [koZnaZna] mapira na `ko_zna_zna`, [winRatio] na `win_ratio` itd.
 */
data class StatsDto(
    val overall: OverallStatsDto,
    val koZnaZna: KoZnaZnaStatsDto,
    val mojBroj: MojBrojStatsDto,
    val korakPoKorak: KorakPoKorakStatsDto,
    val asocijacije: AsocijacijeStatsDto,
    val skocko: SkockoStatsDto,
    val spojnice: SpojniceStatsDto
)

data class OverallStatsDto(
    val totalGames: Int,
    val wins: Int,
    val losses: Int,
    val winRatio: Double
)

data class KoZnaZnaStatsDto(val correct: Int, val missed: Int)

data class MojBrojStatsDto(val totalAttempts: Int, val exactHits: Int)

data class KorakPoKorakStatsDto(val guessedAtStep: List<Int>, val failed: Int)

data class AsocijacijeStatsDto(val solved: Int, val unsolved: Int)

data class SkockoStatsDto(val correctAtAttempt: List<Int>, val failed: Int)

data class SpojniceStatsDto(val total: Int, val successful: Int)
