package rs.tim13.slagalica.notifications

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import rs.tim13.slagalica.R
import rs.tim13.slagalica.core.NotificationHelper
import rs.tim13.slagalica.notifications.data.FcmTokenRegistrar
import rs.tim13.slagalica.notifications.model.NotificationCategory

/**
 * Prima FCM push poruke (spec 11). Backend šalje data-only poruke (category/title/body), pa app
 * sam gradi notifikaciju na tačnom kanalu — i kad je u prvom planu i kad je u pozadini/ugašena.
 */
class SlagalicaFcmService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        // Uređaj je dobio novi token — veži ga za nalog ako je korisnik ulogovan.
        FcmTokenRegistrar.register(applicationContext, token)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        val data = message.data
        val category = runCatching {
            NotificationCategory.valueOf(data["category"] ?: "OSTALO")
        }.getOrDefault(NotificationCategory.OSTALO)
        val title = data["title"] ?: getString(R.string.app_name)
        val body = data["body"].orEmpty()

        NotificationHelper.show(applicationContext, category, title, body)
    }
}
