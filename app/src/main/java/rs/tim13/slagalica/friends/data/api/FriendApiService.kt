package rs.tim13.slagalica.friends.data.api

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import rs.tim13.slagalica.friends.data.api.dto.FriendDto
import rs.tim13.slagalica.friends.data.api.dto.FriendRequestDto
import rs.tim13.slagalica.friends.data.api.dto.GameInviteDto
import rs.tim13.slagalica.friends.data.api.dto.InviteCreatedDto
import rs.tim13.slagalica.friends.data.api.dto.MessageDto
import rs.tim13.slagalica.friends.data.api.dto.SearchUserDto
import rs.tim13.slagalica.friends.data.api.dto.SendFriendRequestBody
import rs.tim13.slagalica.friends.data.api.dto.SendInviteBody

/** REST sloj za prijatelje (spec 7). Pozivi za partiju idu i preko socketa (vidi SocketManager). */
interface FriendApiService {
    @GET("/api/friends")
    suspend fun getFriends(): Response<List<FriendDto>>

    @GET("/api/friends/search")
    suspend fun searchUsers(@Query("q") query: String): Response<List<SearchUserDto>>

    @POST("/api/friends/requests")
    suspend fun sendFriendRequest(@Body body: SendFriendRequestBody): Response<MessageDto>

    @GET("/api/friends/requests")
    suspend fun getFriendRequests(): Response<List<FriendRequestDto>>

    @POST("/api/friends/requests/{id}/accept")
    suspend fun acceptFriendRequest(@Path("id") id: Int): Response<MessageDto>

    @POST("/api/friends/requests/{id}/decline")
    suspend fun declineFriendRequest(@Path("id") id: Int): Response<MessageDto>

    @DELETE("/api/friends/{id}")
    suspend fun removeFriend(@Path("id") id: Int): Response<MessageDto>

    @POST("/api/friends/invites")
    suspend fun sendGameInvite(@Body body: SendInviteBody): Response<InviteCreatedDto>

    @GET("/api/friends/invites")
    suspend fun getGameInvites(): Response<List<GameInviteDto>>

    @POST("/api/friends/invites/{id}/accept")
    suspend fun acceptGameInvite(@Path("id") id: Int): Response<MessageDto>

    @POST("/api/friends/invites/{id}/decline")
    suspend fun declineGameInvite(@Path("id") id: Int): Response<MessageDto>

    @DELETE("/api/friends/invites/{id}")
    suspend fun cancelGameInvite(@Path("id") id: Int): Response<MessageDto>
}
