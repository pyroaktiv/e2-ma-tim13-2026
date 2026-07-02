package rs.tim13.slagalica.notifications.data

import android.content.Context
import com.google.android.gms.tasks.Tasks
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import rs.tim13.slagalica.core.network.RetrofitClient
import rs.tim13.slagalica.core.util.TokenManager

/**
 * Veže FCM push token uređaja za nalog na backendu (spec 11) — tako uređaj „zna" nalog i može
 * primati notifikacije i kad app nije u prvom planu. Svi Firebase pozivi su omotani u [runCatching]
 * da odsustvo `google-services.json`-a ne obori aplikaciju (FCM tada radi kao no-op).
 */
object FcmTokenRegistrar {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /** Pri startu/loginu — pribavi trenutni token i registruj ga ako je korisnik ulogovan. */
    fun syncIfLoggedIn(context: Context) {
        val appCtx = context.applicationContext
        if (!TokenManager(appCtx).isLoggedIn()) return
        runCatching {
            FirebaseMessaging.getInstance().token.addOnSuccessListener { token ->
                if (!token.isNullOrEmpty()) register(appCtx, token)
            }
        }
    }

    /** Registruje konkretan token (poziva se i iz [rs.tim13.slagalica.notifications.SlagalicaFcmService.onNewToken]). */
    fun register(context: Context, token: String) {
        val appCtx = context.applicationContext
        if (!TokenManager(appCtx).isLoggedIn()) return
        scope.launch {
            runCatching { api(appCtx).registerFcmToken(FcmTokenRequest(token)) }
        }
    }

    /**
     * Odjava trenutnog tokena sa backenda. Pozvati PRE brisanja JWT-a (zahtev traži autentifikaciju).
     * Suspend: čeka token na IO niti, pa poziva backend.
     */
    suspend fun unregisterCurrent(context: Context) {
        val appCtx = context.applicationContext
        val token = withContext(Dispatchers.IO) {
            runCatching { Tasks.await(FirebaseMessaging.getInstance().token) }.getOrNull()
        } ?: return
        runCatching { api(appCtx).unregisterFcmToken(FcmTokenRequest(token)) }
    }

    private fun api(context: Context) =
        RetrofitClient.getClient(context).create(NotificationApiService::class.java)
}
