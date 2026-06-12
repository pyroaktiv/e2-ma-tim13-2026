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

    fun saveUser(id: Int, username: String) {
        prefs.edit()
            .putInt("user_id", id)
            .putString("username", username)
            .apply()
    }

    fun getUserId(): Int = prefs.getInt("user_id", -1)

    fun getUsername(): String? = prefs.getString("username", null)

    fun clearUser() {
        prefs.edit().remove("user_id").remove("username").apply()
    }
}