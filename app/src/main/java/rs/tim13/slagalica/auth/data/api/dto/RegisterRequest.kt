package rs.tim13.slagalica.auth.data.api.dto

data class RegisterRequest(
    val email: String,
    val username: String,
    val region: String, // TODO
    val password: String,
    val confirmPassword: String
)
