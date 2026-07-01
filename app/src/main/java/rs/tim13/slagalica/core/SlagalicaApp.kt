package rs.tim13.slagalica.core

import android.app.Application

/**
 * Aplikacioni entry-point. Notifikacioni kanali (spec 11.a) se prave na startu aplikacije,
 * pre nego što bilo koja komponenta pokuša da prikaže notifikaciju (npr. FCM servis dok je
 * aplikacija u pozadini). Firebase se auto-inicijalizuje preko google-services plugina.
 */
class SlagalicaApp : Application() {
    override fun onCreate() {
        super.onCreate()
        NotificationHelper.createChannels(this)
    }
}
