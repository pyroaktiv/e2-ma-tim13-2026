package rs.tim13.slagalica.regions.data.api

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import rs.tim13.slagalica.regions.data.api.dto.RegionDto
import rs.tim13.slagalica.regions.data.api.dto.RegionMapPointDto
import rs.tim13.slagalica.regions.data.api.dto.RegionStatsDto

/** REST sloj za prikaz regiona (spec 5) — backend rute Studenta 3. */
interface RegionApiService {
    @GET("/api/regions")
    suspend fun getRegions(): Response<List<RegionDto>>

    @GET("/api/regions/map")
    suspend fun getMap(): Response<List<RegionMapPointDto>>

    @GET("/api/regions/{name}/stats")
    suspend fun getStats(@Path("name") name: String): Response<RegionStatsDto>
}
