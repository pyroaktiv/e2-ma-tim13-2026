package rs.tim13.slagalica.profil.data.api.dto

/**
 * Profil registrovanog korisnika sa `/api/user/profile`. Gson koristi LOWER_CASE_WITH_UNDERSCORES,
 * pa se [totalStars] mapira na `total_stars`, [qrToken] na `qr_token` itd.
 */
data class ProfileDto(
    val id: Int,
    val username: String,
    val email: String,
    val avatar: String,
    val tokens: Int,
    val totalStars: Int,
    val league: LeagueDto,
    val region: String,
    val qrToken: String
)

data class LeagueDto(
    val name: String,
    val icon: String
)
