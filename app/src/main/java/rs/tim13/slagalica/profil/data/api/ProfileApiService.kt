package rs.tim13.slagalica.profil.data.api

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PUT
import rs.tim13.slagalica.profil.data.api.dto.AvatarUpdateRequest
import rs.tim13.slagalica.profil.data.api.dto.AvatarUpdateResponse
import rs.tim13.slagalica.profil.data.api.dto.ProfileDto
import rs.tim13.slagalica.profil.data.api.dto.StatsDto

interface ProfileApiService {
    @GET("/api/user/profile")
    suspend fun getProfile(): Response<ProfileDto>

    @GET("/api/user/stats")
    suspend fun getStats(): Response<StatsDto>

    @PUT("/api/user/avatar")
    suspend fun updateAvatar(@Body body: AvatarUpdateRequest): Response<AvatarUpdateResponse>
}
