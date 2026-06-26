package rs.tim13.slagalica.dailymissions.data

import retrofit2.Call
import retrofit2.http.GET
import rs.tim13.slagalica.dailymissions.model.DailyMissionsResponse

interface DailyMissionsApiService {
    @GET("api/missions/daily")
    fun getDailyMissions(): Call<DailyMissionsResponse>
}
