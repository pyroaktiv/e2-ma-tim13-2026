package rs.tim13.slagalica.notifications.data

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.HTTP
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import rs.tim13.slagalica.leaderboard.model.NotificationDto

/** Telo zahteva za (od)registraciju FCM push tokena uređaja (spec 11). */
data class FcmTokenRequest(val token: String)

/**
 * REST pristup istoriji notifikacija (spec 11.b/c) i registraciji FCM tokena.
 * Suspend varijante — koristi ih [rs.tim13.slagalica.notifications.ui.NotificationsViewModel].
 */
interface NotificationApiService {

    @GET("api/notifications")
    suspend fun getNotifications(): List<NotificationDto>

    @PATCH("api/notifications/{id}/read")
    suspend fun markRead(@Path("id") id: Long): Response<Unit>

    @POST("api/notifications/fcm-token")
    suspend fun registerFcmToken(@Body body: FcmTokenRequest): Response<Unit>

    @HTTP(method = "DELETE", path = "api/notifications/fcm-token", hasBody = true)
    suspend fun unregisterFcmToken(@Body body: FcmTokenRequest): Response<Unit>
}
