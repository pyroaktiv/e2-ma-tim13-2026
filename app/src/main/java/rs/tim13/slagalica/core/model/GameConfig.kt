package rs.tim13.slagalica.core.model

import android.os.Bundle

/**
 * Konfiguracija sa kojom koordinator pokreće jednu igru.
 *
 * Ovo je tačka spajanja (seam) između budućeg koordinatora partije i pojedinačnih igara:
 * koordinator kreira fragment igre, ubaci [toBundle] kao argumente, a fragment pročita
 * konfiguraciju preko [fromBundle] i prosledi je odgovarajućem ViewModel factory-ju.
 *
 * Podrazumevane vrednosti opisuju samostalnu (single-player) partiju, što omogućava da se
 * igra otvori i bez koordinatora tokom razvoja.
 */
data class GameConfig(
    val localPlayer: Player = Player.BLUE,
    val isSinglePlayer: Boolean = true,
    val initialOpponentDisconnected: Boolean = false
) {
    fun toBundle(): Bundle = Bundle().apply {
        putString(KEY_LOCAL_PLAYER, localPlayer.name)
        putBoolean(KEY_SINGLE_PLAYER, isSinglePlayer)
        putBoolean(KEY_OPPONENT_DISCONNECTED, initialOpponentDisconnected)
    }

    companion object {
        private const val KEY_LOCAL_PLAYER = "game_config_local_player"
        private const val KEY_SINGLE_PLAYER = "game_config_single_player"
        private const val KEY_OPPONENT_DISCONNECTED = "game_config_opponent_disconnected"

        fun fromBundle(args: Bundle?): GameConfig {
            if (args == null) return GameConfig()
            return GameConfig(
                localPlayer = args.getString(KEY_LOCAL_PLAYER)
                    ?.let { runCatching { Player.valueOf(it) }.getOrNull() }
                    ?: Player.BLUE,
                isSinglePlayer = args.getBoolean(KEY_SINGLE_PLAYER, true),
                initialOpponentDisconnected = args.getBoolean(KEY_OPPONENT_DISCONNECTED, false)
            )
        }
    }
}
