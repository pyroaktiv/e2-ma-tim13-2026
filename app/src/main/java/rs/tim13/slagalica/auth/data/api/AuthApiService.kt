package rs.tim13.slagalica.auth.data.api

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

interface AuthApiService {
    @POST("/api/auth/login")
    fun login(@Body body: LoginRequest): Call<LoginResponse>

    @POST("/api/auth/register")
    fun register(@Body body: RegisterRequest): Call<MessageResponse>

    @POST("/api/auth/reset-password")
    fun resetPassword(@Body body: ResetPasswordRequest): Call<MessageResponse>

    @GET("/api/user/profile")
    fun getProfile(): Call<ProfileResponse>

    @GET("/api/user/stats")
    fun getStats(): Call<StatsResponse>

    @PUT("/api/user/avatar")
    fun updateAvatar(@Body body: AvatarRequest): Call<MessageResponse>

    @GET("/api/notifications")
    fun getNotifications(): Call<List<NotificationDto>>

    @PATCH("/api/notifications/{id}/read")
    fun markNotificationRead(@Path("id") id: Long): Call<MessageResponse>
}
