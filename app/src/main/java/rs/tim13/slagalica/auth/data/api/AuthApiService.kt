package rs.tim13.slagalica.auth.data.api

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface AuthApiService {
    @POST("/api/auth/login")
    fun login(@Body body: LoginRequest): Call<LoginResponse>

    @GET("/api/user/profile")
    fun getProfile(): Call<ProfileResponse>

    @GET("/api/user/stats")
    fun getStats(): Call<StatsResponse>
}
