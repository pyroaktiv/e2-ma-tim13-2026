package rs.tim13.slagalica.asocijacije.data

import rs.tim13.slagalica.asocijacije.model.AssociationsColumn
import rs.tim13.slagalica.asocijacije.model.AssociationsField
import rs.tim13.slagalica.asocijacije.model.AssociationsGame
import rs.tim13.slagalica.asocijacije.model.AssociationsGameRepository
import rs.tim13.slagalica.core.network.socket.AssociationsGameDto

/** Repozitorijum Asocijacija čiji sadržaj dolazi sa servera. */
class RemoteAssociationsGameRepository(
    private val games: List<AssociationsGameDto>
) : AssociationsGameRepository {
    override fun getGames(): List<AssociationsGame> = games.map { dto ->
        AssociationsGame(
            columns = dto.columns.map { col ->
                AssociationsColumn(
                    index = col.index,
                    fields = col.fields.map { AssociationsField(it) },
                    solution = col.solution
                )
            },
            finalSolution = dto.finalSolution
        )
    }
}
