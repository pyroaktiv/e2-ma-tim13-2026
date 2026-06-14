package rs.tim13.slagalica.profil.data.api

import retrofit2.Response
import retrofit2.http.GET
import rs.tim13.slagalica.profil.data.api.dto.ProfileDto

interface ProfileApiService {
    @GET("/api/user/profile")
    suspend fun getProfile(): Response<ProfileDto>
}
