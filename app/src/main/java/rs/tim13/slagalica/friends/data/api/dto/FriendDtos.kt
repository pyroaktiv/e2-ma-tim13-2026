package rs.tim13.slagalica.friends.data.api.dto

import rs.tim13.slagalica.profil.data.api.dto.LeagueDto

/**
 * DTO-i za sistem prijatelja (spec 7). Retrofit Gson koristi LOWER_CASE_WITH_UNDERSCORES,
 * pa se npr. [totalStars] mapira na `total_stars`, [toUserId] na `to_user_id` itd.
 */

data class FriendDto(
    val id: Int,
    val username: String,
    val avatar: String,
    val totalStars: Int,
    val league: LeagueDto,
    val monthlyRank: Int?,
    val isOnline: Boolean,
    val inGame: Boolean,
    val friendshipId: Int
)

/** Rezultat pretrage korisnika; `relationship` je none | pending_sent | pending_received | friends. */
data class SearchUserDto(
    val id: Int,
    val username: String,
    val avatar: String,
    val relationship: String
)

data class UserBriefDto(
    val id: Int,
    val username: String,
    val avatar: String?
)

data class FriendRequestDto(
    val id: Int,
    val from: UserBriefDto,
    val createdAt: String
)

data class GameInviteDto(
    val id: Int,
    val from: UserBriefDto,
    val createdAt: String,
    val expiresAt: String
)

data class InviteCreatedDto(
    val id: Int,
    val expiresAt: String
)

/** Telo za slanje zahteva za prijateljstvo — po imenu ili QR tokenu (spec 7.b). */
data class SendFriendRequestBody(
    val username: String? = null,
    val qrToken: String? = null
)

data class SendInviteBody(val toUserId: Int)

/** Generička poruka odgovora servera. */
data class MessageDto(val message: String? = null, val error: String? = null)
