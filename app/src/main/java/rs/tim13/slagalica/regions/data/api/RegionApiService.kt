package rs.tim13.slagalica.regions.data.api

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query
import rs.tim13.slagalica.regions.data.api.dto.RegionMapPointDto
import rs.tim13.slagalica.regions.data.api.dto.RegionRankingDto
import rs.tim13.slagalica.regions.data.api.dto.RegionStatsDto

/** REST sloj za prikaz regiona (spec 5). */
interface RegionApiService {
    @GET("/api/regions/ranking")
    suspend fun getRanking(): Response<RegionRankingDto>

    @GET("/api/regions/map")
    suspend fun getMap(): Response<List<RegionMapPointDto>>

    @GET("/api/regions/stats")
    suspend fun getStats(@Query("region") region: String): Response<RegionStatsDto>
}
