package rs.tim13.slagalica.spojnice.data

import rs.tim13.slagalica.core.network.socket.SpojniceRoundDto
import rs.tim13.slagalica.spojnice.model.SpojniceRound

/** Repozitorijum Spojnica čiji sadržaj dolazi sa servera. */
class RemoteSpojniceGameRepository(
    private val rounds: List<SpojniceRoundDto>
) : SpojniceGameRepository {
    override fun getRounds(): List<SpojniceRound> = rounds.map { dto ->
        SpojniceRound(
            leftItems = dto.leftItems,
            rightItems = dto.rightItems,
            solution = dto.solution.indices.associateWith { dto.solution[it] }
        )
    }
}
