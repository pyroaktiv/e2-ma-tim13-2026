package rs.tim13.slagalica.auth.data.api.dto

data class ResetPasswordRequest(val oldPassword: String, val newPassword: String, val newPasswordConfirm: String)
