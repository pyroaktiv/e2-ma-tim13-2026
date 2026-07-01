package rs.tim13.slagalica.regions.data.api.dto

/**
 * DTO-i za prikaz regiona (spec 5) — usklađeni sa backend rutama Studenta 3.
 * Retrofit Gson koristi LOWER_CASE_WITH_UNDERSCORES (npr. [monthlyStars] -> `monthly_stars`).
 */

/** Stavka mesečne rang liste regiona — `GET /api/regions`. */
data class RegionDto(
    val name: String,
    val icon: String,
    val monthlyStars: Int,
    val rank: Int,
    val totalPlayers: Int,
    val isOwnRegion: Boolean
)

/** Tačka igrača na mapi — `GET /api/regions/map`. */
data class RegionMapPointDto(
    val region: String,
    val lat: Double,
    val lng: Double
)

/** Statistika regiona — `GET /api/regions/{name}/stats`. */
data class RegionStatsDto(
    val name: String,
    val icon: String,
    val firstPlaceCount: Int,
    val secondPlaceCount: Int,
    val thirdPlaceCount: Int,
    val activePlayers: Int,
    val totalPlayers: Int,
    val currentMonthlyStars: Int
)
