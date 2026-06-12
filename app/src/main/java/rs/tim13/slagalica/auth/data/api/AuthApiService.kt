package rs.tim13.slagalica.auth.data.api

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST
import rs.tim13.slagalica.auth.data.api.dto.LoginRequest
import rs.tim13.slagalica.auth.data.api.dto.LoginResponse
import rs.tim13.slagalica.auth.data.api.dto.RegisterRequest
import rs.tim13.slagalica.auth.data.api.dto.RegisterResponse
import rs.tim13.slagalica.auth.data.api.dto.ResetPasswordRequest
import rs.tim13.slagalica.auth.data.api.dto.ResetPasswordResponse

interface AuthApiService {
    @POST("/api/auth/login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    @POST("/api/auth/register")
    suspend fun register(@Body request: RegisterRequest): Response<RegisterResponse>

    @POST("/api/auth/reset-password")
    suspend fun resetPassword(@Body request: ResetPasswordRequest) : Response<ResetPasswordResponse>
}