package rs.tim13.slagalica.core.util

import android.graphics.Color
import androidx.annotation.DrawableRes
import rs.tim13.slagalica.R

/**
 * Mapiranje stringova sa servera (naziv avatara, ikona lige) na lokalne resurse i boje.
 * Deljeno između profila (spec 2), prijatelja (7) i regiona (5).
 */
object ProfileResources {

    @DrawableRes
    fun avatarDrawable(avatar: String?): Int = when (avatar) {
        "avatar_01" -> R.drawable.avatar_01
        "avatar_02" -> R.drawable.avatar_02
        "avatar_03" -> R.drawable.avatar_03
        "avatar_04" -> R.drawable.avatar_04
        "avatar_05" -> R.drawable.avatar_05
        "avatar_06" -> R.drawable.avatar_06
        "avatar_07" -> R.drawable.avatar_07
        "avatar_08" -> R.drawable.avatar_08
        "avatar_09" -> R.drawable.avatar_09
        "avatar_10" -> R.drawable.avatar_10
        else -> R.drawable.avatar_default
    }

    /** Spisak svih avatara koje korisnik može izabrati (spec 2.b). */
    val selectableAvatars: List<String> = (1..10).map { "avatar_%02d".format(it) }

    @DrawableRes
    fun leagueIcon(icon: String?): Int = when (icon) {
        "league_bronze" -> R.drawable.league_bronze
        "league_silver" -> R.drawable.league_silver
        "league_gold" -> R.drawable.league_gold
        "league_platinum" -> R.drawable.league_platinum
        "league_diamond" -> R.drawable.league_diamond
        "league_master" -> R.drawable.league_master
        else -> R.drawable.league_bronze
    }

    /** Boja okvira avatara na osnovu lige (spec 2.a.iii). Region top-3 (spec 5.e) je posebno. */
    fun leagueColor(icon: String?): Int = when (icon) {
        "league_bronze" -> Color.parseColor("#CD7F32")
        "league_silver" -> Color.parseColor("#BDBDBD")
        "league_gold" -> Color.parseColor("#FFC107")
        "league_platinum" -> Color.parseColor("#B0BEC5")
        "league_diamond" -> Color.parseColor("#4DD0E1")
        "league_master" -> Color.parseColor("#7E57C2")
        else -> Color.parseColor("#CD7F32")
    }

    @DrawableRes
    fun regionIcon(iconKey: String?): Int = when (iconKey) {
        "region_banat" -> R.drawable.region_banat
        "region_backa" -> R.drawable.region_backa
        "region_srem" -> R.drawable.region_srem
        "region_centralna" -> R.drawable.region_centralna
        "region_kosovo" -> R.drawable.region_kosovo
        else -> R.drawable.region_centralna
    }

    @DrawableRes
    fun regionIconByName(name: String?): Int = when (name) {
        "Banat" -> R.drawable.region_banat
        "Bačka" -> R.drawable.region_backa
        "Srem" -> R.drawable.region_srem
        "Centralna Srbija" -> R.drawable.region_centralna
        "Kosovo" -> R.drawable.region_kosovo
        else -> R.drawable.region_centralna
    }

    /** Zlatna/srebrna/bronzana boja okvira za top-3 region prethodnog ciklusa (spec 5.e); null inače. */
    fun regionMedalColor(medal: Int?): Int? = when (medal) {
        1 -> Color.parseColor("#FFD700")
        2 -> Color.parseColor("#C0C0C0")
        3 -> Color.parseColor("#CD7F32")
        else -> null
    }

    /** Deterministička boja čiode/ikone regiona iz naziva (podržava proizvoljan broj regiona). */
    fun regionColor(name: String?): Int {
        val hue = (((name ?: "").hashCode() % 360) + 360) % 360
        return Color.HSVToColor(floatArrayOf(hue.toFloat(), 0.65f, 0.85f))
    }
}
