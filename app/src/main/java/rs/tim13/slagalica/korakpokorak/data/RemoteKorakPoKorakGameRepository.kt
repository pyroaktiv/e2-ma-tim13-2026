package rs.tim13.slagalica.korakpokorak.data

import rs.tim13.slagalica.core.network.socket.KorakPoKorakRoundDto
import rs.tim13.slagalica.korakpokorak.model.KorakPoKorakRound

/** Repozitorijum „Korak po korak" čiji sadržaj dolazi sa servera. */
class RemoteKorakPoKorakGameRepository(
    private val rounds: List<KorakPoKorakRoundDto>
) : KorakPoKorakGameRepository {
    override fun getRounds(): List<KorakPoKorakRound> =
        rounds.map { KorakPoKorakRound(clues = it.clues, solution = it.solution) }
}
