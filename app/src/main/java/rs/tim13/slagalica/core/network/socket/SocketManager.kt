package rs.tim13.slagalica.core.network.socket

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.gson.Gson
import com.google.gson.JsonObject
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import rs.tim13.slagalica.BuildConfig
import rs.tim13.slagalica.core.util.TokenManager

/**
 * Jedan deljeni WebSocket prema backendu (`/ws`). Most je između OkHttp callback-ova i
 * ostatka aplikacije: dolazne poruke izlaže kao [ServerMessage] preko [incoming] LiveData,
 * a odlazne prima kao [ClientMessage] u [send]. Registrovan korisnik se povezuje sa JWT-om,
 * a gost preko `?guest=1` (server mu dodeli privremeni identitet).
 */
object SocketManager {

    private val gson = Gson()
    private val client = OkHttpClient()
    private var webSocket: WebSocket? = null

    private val _incoming = MutableLiveData<ServerMessage>()
    val incoming: LiveData<ServerMessage> = _incoming

    private val _connected = MutableLiveData(false)
    val connected: LiveData<Boolean> = _connected

    var isGuest: Boolean = false
        private set

    fun connect(context: Context) {
        if (webSocket != null) return

        // Očisti eventualnu zaostalu poruku prethodne partije (LiveData ponavlja poslednju vrednost).
        _incoming.postValue(ServerMessage.Idle)

        val token = TokenManager(context).getToken()
        isGuest = token == null

        val base = BuildConfig.API_BASE_URL.trimEnd('/')
        val wsBase = when {
            base.startsWith("https") -> base.replaceFirst("https", "wss")
            base.startsWith("http") -> base.replaceFirst("http", "ws")
            else -> base
        }
        val wsUrl = if (token != null) "$wsBase/ws?token=$token" else "$wsBase/ws?guest=1"

        webSocket = client.newWebSocket(Request.Builder().url(wsUrl).build(), listener)
    }

    fun send(message: ClientMessage) {
        webSocket?.send(gson.toJson(message))
    }

    fun disconnect() {
        webSocket?.close(NORMAL_CLOSURE, null)
        webSocket = null
        _connected.postValue(false)
    }

    private val listener = object : WebSocketListener() {
        override fun onOpen(webSocket: WebSocket, response: Response) {
            _connected.postValue(true)
        }

        override fun onMessage(webSocket: WebSocket, text: String) {
            parse(text)?.let { _incoming.postValue(it) }
        }

        override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
            this@SocketManager.webSocket = null
            _connected.postValue(false)
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            this@SocketManager.webSocket = null
            _connected.postValue(false)
        }
    }

    private fun parse(text: String): ServerMessage? {
        val obj = runCatching { gson.fromJson(text, JsonObject::class.java) }.getOrNull() ?: return null
        return when (obj.get("type")?.asString) {
            "match_found" -> gson.fromJson(text, ServerMessage.MatchFound::class.java)
            "match_move" -> gson.fromJson(text, ServerMessage.RemoteMove::class.java)
            "opponent_left" -> ServerMessage.OpponentLeft
            "match_over" -> gson.fromJson(text, ServerMessage.MatchOver::class.java)
            "error" -> gson.fromJson(text, ServerMessage.ServerError::class.java)
            "challenge_created" -> gson.fromJson(text, ServerMessage.ChallengeCreated::class.java)
            "challenge_update" -> gson.fromJson(text, ServerMessage.ChallengeUpdate::class.java)
            "challenge_cancelled" -> gson.fromJson(text, ServerMessage.ChallengeCancelled::class.java)
            "challenge_started" -> gson.fromJson(text, ServerMessage.ChallengeStarted::class.java)
            "challenge_over" -> gson.fromJson(text, ServerMessage.ChallengeOver::class.java)
            "chat_message"      -> gson.fromJson(text, ServerMessage.ChatMessage::class.java)
            "mission_progress"  -> gson.fromJson(text, ServerMessage.MissionProgress::class.java)
            "mission_bonus"     -> gson.fromJson(text, ServerMessage.MissionBonus::class.java)
            "tournament_queued"         -> ServerMessage.TournamentQueued
            "tournament_cancelled"      -> ServerMessage.TournamentCancelled
            "tournament_found"          -> gson.fromJson(text, ServerMessage.TournamentFound::class.java)
            "tournament_semi_over"      -> gson.fromJson(text, ServerMessage.TournamentSemiOver::class.java)
            "tournament_final_started"  -> gson.fromJson(text, ServerMessage.TournamentFinalStarted::class.java)
            "tournament_update"         -> gson.fromJson(text, ServerMessage.TournamentUpdate::class.java)
            "tournament_over"           -> gson.fromJson(text, ServerMessage.TournamentOver::class.java)
            else -> null
        }
    }

    private const val NORMAL_CLOSURE = 1000
}
