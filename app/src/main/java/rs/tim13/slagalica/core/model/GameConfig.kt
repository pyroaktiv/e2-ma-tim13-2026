package rs.tim13.slagalica.core.model

import android.os.Bundle

/**
 * Konfiguracija sa kojom koordinator pokreće jednu igru.
 *
 * Ovo je tačka spajanja (seam) između koordinatora partije i pojedinačnih igara:
 * koordinator kreira fragment igre, ubaci [toBundle] kao argumente, a fragment pročita
 * konfiguraciju preko [fromBundle] i prosledi je odgovarajućem ViewModel factory-ju.
 *
 * Podrazumevane vrednosti opisuju samostalnu (single-player) partiju, što omogućava da se
 * igra otvori i bez koordinatora tokom razvoja.
 */
data class GameConfig(
    val localPlayer: Player = Player.BLUE,
    val isSinglePlayer: Boolean = true,
    val initialOpponentDisconnected: Boolean = false,
    val tokens: Int = 0,
    val stars: Int = 0
) {
    fun toBundle(): Bundle = Bundle().apply {
        putString(KEY_LOCAL_PLAYER, localPlayer.name)
        putBoolean(KEY_SINGLE_PLAYER, isSinglePlayer)
        putBoolean(KEY_OPPONENT_DISCONNECTED, initialOpponentDisconnected)
        putInt(KEY_TOKENS, tokens)
        putInt(KEY_STARS, stars)
    }

    companion object {
        private const val KEY_LOCAL_PLAYER = "game_config_local_player"
        private const val KEY_SINGLE_PLAYER = "game_config_single_player"
        private const val KEY_OPPONENT_DISCONNECTED = "game_config_opponent_disconnected"
        private const val KEY_TOKENS = "game_config_tokens"
        private const val KEY_STARS = "game_config_stars"

        fun fromBundle(args: Bundle?): GameConfig {
            if (args == null) return GameConfig()
            return GameConfig(
                localPlayer = args.getString(KEY_LOCAL_PLAYER)
                    ?.let { runCatching { Player.valueOf(it) }.getOrNull() }
                    ?: Player.BLUE,
                isSinglePlayer = args.getBoolean(KEY_SINGLE_PLAYER, true),
                initialOpponentDisconnected = args.getBoolean(KEY_OPPONENT_DISCONNECTED, false),
                tokens = args.getInt(KEY_TOKENS, 0),
                stars = args.getInt(KEY_STARS, 0)
            )
        }
    }
}
