package rs.tim13.slagalica.mojbroj.data

import rs.tim13.slagalica.core.network.socket.MojBrojRoundDto
import rs.tim13.slagalica.mojbroj.model.MojBrojRound

/** Repozitorijum „Moj broj" čije zagonetke dolaze sa servera. */
class RemoteMojBrojGameRepository(
    private val rounds: List<MojBrojRoundDto>
) : MojBrojGameRepository {
    override fun getRounds(): List<MojBrojRound> =
        rounds.map { MojBrojRound(target = it.target, numbers = it.numbers) }
}
