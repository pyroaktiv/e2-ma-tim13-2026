package rs.tim13.slagalica.izazov.data.api

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import rs.tim13.slagalica.core.network.socket.ChallengeDto

interface ChallengeApiService {
    @GET("/api/challenges")
    suspend fun listChallenges(): Response<List<ChallengeDto>>

    @GET("/api/challenges/{id}")
    suspend fun getChallenge(@Path("id") id: String): Response<ChallengeDto>
}
