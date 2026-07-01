package rs.tim13.slagalica.core.util

import android.content.Context
import android.content.SharedPreferences

class TokenManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("slagalica_prefs", Context.MODE_PRIVATE)

    fun saveToken(token: String) {
        prefs.edit().putString("jwt_token", token).apply()
    }

    fun getToken(): String? {
        return prefs.getString("jwt_token", null)
    }

    fun clearToken() {
        prefs.edit().remove("jwt_token").apply()
    }

    fun getUserId(): Int {
        val token = getToken() ?: return 0
        return payload(token)?.optString("sub", "0")?.toIntOrNull() ?: 0
    }

    /** Sačuvan token koji nije istekao — uslov da uređaj „zna" nalog i preskoči login (spec 11). */
    fun isLoggedIn(): Boolean {
        val token = getToken() ?: return false
        val exp = payload(token)?.optLong("exp", 0L) ?: return false
        // exp je u sekundama; 0 (nema claima) tretiramo kao nevažeći.
        return exp > 0L && exp > System.currentTimeMillis() / 1000
    }

    private fun payload(token: String): org.json.JSONObject? {
        return try {
            val part = token.split(".")[1]
            val decoded = String(android.util.Base64.decode(part, android.util.Base64.URL_SAFE or android.util.Base64.NO_PADDING))
            org.json.JSONObject(decoded)
        } catch (e: Exception) {
            null
        }
    }
}