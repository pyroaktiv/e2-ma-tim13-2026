package rs.tim13.slagalica.profil.data.api.dto

/** Telo zahteva za izmenu avatara — `PUT /api/user/avatar` (spec 2.b). */
data class AvatarUpdateRequest(val avatar: String)

/** Odgovor servera nakon izmene avatara. */
data class AvatarUpdateResponse(val message: String, val avatar: String)
