package rs.tim13.slagalica.dailymissions.data

import rs.tim13.slagalica.dailymissions.model.DailyMissionsResponse

interface DailyMissionsRepository {
    fun get(callback: (DailyMissionsResponse?, String?) -> Unit)
}
