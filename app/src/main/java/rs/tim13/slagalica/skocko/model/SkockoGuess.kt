package rs.tim13.slagalica.skocko.model

data class SkockoGuess(
    val symbols: List<SkockoSymbol>,
    val hints: List<SkockoHint>
)
