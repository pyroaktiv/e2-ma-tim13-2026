package rs.tim13.slagalica.regions.data.api.dto

/**
 * DTO-i za prikaz regiona (spec 5). Retrofit Gson koristi LOWER_CASE_WITH_UNDERSCORES,
 * pa se npr. [myRegion] mapira na `my_region`, [isMine] na `is_mine` itd.
 */

data class RegionRankingDto(
    val cycle: String,
    val start: String,
    val end: String,
    val myRegion: String?,
    val previousTop3: List<String>,
    val regions: List<RegionRankEntryDto>
)

data class RegionRankEntryDto(
    val region: String,
    val icon: String,
    val stars: Int,
    val rank: Int,
    val isMine: Boolean
)

data class RegionMapPointDto(
    val id: Int,
    val username: String,
    val avatar: String,
    val region: String,
    val lat: Double,
    val lng: Double
)

data class RegionStatsDto(
    val region: String,
    val icon: String,
    val firstPlaces: Int,
    val secondPlaces: Int,
    val thirdPlaces: Int,
    val activePlayers: Int,
    val registeredPlayers: Int
)
