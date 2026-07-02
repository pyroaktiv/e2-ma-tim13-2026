package rs.tim13.slagalica.match

/**
 * Izlaže [MatchViewModel] dečjim game-fragmentima, koji preko njega dobijaju repozitorijum
 * i [rs.tim13.slagalica.core.model.GameConfig] za tekuću partiju.
 */
interface MatchHost {
    val match: MatchViewModel
}
