package rs.tim13.slagalica.leaderboard.model

import com.google.gson.annotations.SerializedName

data class NotificationDto(
    @SerializedName("id")         val id: Long,
    @SerializedName("category")   val category: String,
    @SerializedName("title")      val title: String,
    @SerializedName("body")       val body: String,
    @SerializedName("timestamp")  val timestamp: String,
    @SerializedName("is_read")    val isRead: Boolean
)
