package rs.tim13.slagalica.auth.data.api.dto

data class LoginResponse(val token: String, val user: UserDto)