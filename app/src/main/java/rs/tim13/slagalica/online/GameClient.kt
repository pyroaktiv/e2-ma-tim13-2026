package rs.tim13.slagalica.online

import android.content.Context
import android.os.Handler
import android.os.Looper
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONObject
import rs.tim13.slagalica.BuildConfig
import rs.tim13.slagalica.core.AppState
import rs.tim13.slagalica.core.NotificationHelper
import rs.tim13.slagalica.core.util.TokenManager
import rs.tim13.slagalica.notifications.model.NotificationCategory

/**
 * Single shared WebSocket connection to the backend game server.
 *
 * The connection is authenticated with the saved JWT and stays open for the
 * lifetime of the app session. UI components (currently [MatchmakingFragment]'s
 * host) register a [Listener] to receive server events; all callbacks are
 * delivered on the main thread. Synthetic events ("_open", "_closed",
 * "_failure") let the UI react to connection state.
 */
object GameClient {

    fun interface Listener {
        fun onEvent(event: JSONObject)
    }

    private val mainHandler = Handler(Looper.getMainLooper())
    private val client = OkHttpClient()
    private val listeners = mutableListOf<Listener>()

    private var webSocket: WebSocket? = null
    private var appContext: Context? = null

    @Volatile
    var isConnected: Boolean = false
        private set

    fun addListener(listener: Listener) {
        if (!listeners.contains(listener)) listeners.add(listener)
    }

    fun removeListener(listener: Listener) {
        listeners.remove(listener)
    }

    private fun emit(event: JSONObject) {
        mainHandler.post {
            // iterate over a copy so listeners can unregister during dispatch
            for (l in listeners.toList()) l.onEvent(event)
        }
    }

    private fun emitType(type: String) = emit(JSONObject().put("type", type))

    // When the app is in the background, surface key events as OS notifications.
    private fun maybePush(obj: JSONObject) {
        if (AppState.isForeground) return
        val ctx = appContext ?: return
        when (obj.optString("type")) {
            "match_over" -> NotificationHelper.sendNotification(
                ctx, NotificationCategory.NAGRADE, "Partija završena",
                if (obj.optBoolean("youWon")) "Pobedio si! Pogledaj nagrade." else "Partija je gotova."
            )
            "game_invite" -> {
                val from = obj.optJSONObject("invite")?.optJSONObject("from")?.optString("username") ?: "Igrač"
                NotificationHelper.sendNotification(
                    ctx, NotificationCategory.OSTALO, "Poziv za partiju", "$from te poziva na partiju."
                )
            }
        }
    }

    /** Opens the connection if it is not already open. Safe to call repeatedly. */
    fun connect(context: Context) {
        appContext = context.applicationContext
        if (webSocket != null) return
        val token = TokenManager(context.applicationContext).getToken()
        if (token.isNullOrBlank()) {
            emit(JSONObject().put("type", "error").put("message", "Niste prijavljeni"))
            return
        }

        val base = BuildConfig.API_BASE_URL.trim().trim('"')
        val wsBase = base
            .replace("https://", "wss://")
            .replace("http://", "ws://")
            .trimEnd('/')
        val url = "$wsBase/ws?token=$token"

        val request = Request.Builder().url(url).build()
        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                isConnected = true
                emitType("_open")
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                val obj = try {
                    JSONObject(text)
                } catch (_: Exception) {
                    return
                }
                maybePush(obj)
                emit(obj)
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                isConnected = false
                this@GameClient.webSocket = null
                emit(JSONObject().put("type", "_failure").put("message", t.message ?: "Greška veze"))
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                isConnected = false
                this@GameClient.webSocket = null
                emitType("_closed")
            }
        })
    }

    fun send(obj: JSONObject) {
        webSocket?.send(obj.toString())
    }

    fun send(type: String) = send(JSONObject().put("type", type))

    fun sendQuickMatch() = send("quick_match")
    fun sendCancelMatch() = send("cancel_match")
    fun sendLeaveMatch() = send("leave_match")

    fun sendAnswer(answer: Int) =
        send(JSONObject().put("type", "kzz_answer").put("answer", answer))

    fun sendConnect(left: Int, right: Int) =
        send(JSONObject().put("type", "spojnice_connect").put("left", left).put("right", right))

    fun disconnect() {
        webSocket?.close(1000, null)
        webSocket = null
        isConnected = false
    }
}
