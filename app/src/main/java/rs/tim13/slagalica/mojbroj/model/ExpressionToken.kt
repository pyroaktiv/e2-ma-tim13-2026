package rs.tim13.slagalica.mojbroj.model

enum class TokenType { NUMBER, OPERATOR, OPEN_BRACKET, CLOSE_BRACKET }

/**
 * Jedan element matematičkog izraza. Za brojeve, [numberIndex] pamti koji od šest
 * ponuđenih brojeva je iskorišćen (svaki se sme upotrebiti najviše jednom).
 */
data class ExpressionToken(
    val text: String,
    val type: TokenType,
    val numberIndex: Int? = null
)
