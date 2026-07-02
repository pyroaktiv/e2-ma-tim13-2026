package rs.tim13.slagalica.dailymissions.model

data class MissionDto(
    val key: String,
    val title: String,
    val completed: Boolean,
    val starsReward: Int
)

data class BonusDto(
    val allComplete: Boolean,
    val tokensReward: Int,
    val starsReward: Int
)

data class DailyMissionsResponse(
    val date: String,
    val missions: List<MissionDto>,
    val bonus: BonusDto
)
