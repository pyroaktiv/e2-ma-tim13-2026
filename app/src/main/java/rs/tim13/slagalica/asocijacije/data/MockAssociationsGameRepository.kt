package rs.tim13.slagalica.asocijacije.data

import rs.tim13.slagalica.asocijacije.model.AssociationsColumn
import rs.tim13.slagalica.asocijacije.model.AssociationsField
import rs.tim13.slagalica.asocijacije.model.AssociationsGame
import rs.tim13.slagalica.asocijacije.model.AssociationsGameRepository

class MockAssociationsGameRepository : AssociationsGameRepository {

    override fun getGames(): List<AssociationsGame> = listOf(
        AssociationsGame(
            columns = listOf(
                AssociationsColumn(0, fields("NIL", "AMAZON", "DUNAV", "VOLGA"), "REKE"),
                AssociationsColumn(1, fields("TIGAR", "LAV", "GEPARD", "LEOPARD"), "DIVLJE MAČKE"),
                AssociationsColumn(2, fields("EVEREST", "MONT BLAN", "OLIMP", "FUDŽIJAMA"), "PLANINE"),
                AssociationsColumn(3, fields("PERO", "HEMIJSKA", "MARKER", "FLOMASTER"), "PISANJE")
            ),
            finalSolution = "GEOGRAFIJA"
        ),
        AssociationsGame(
            columns = listOf(
                AssociationsColumn(0, fields("JABUKA", "KRUŠKA", "ŠLJIVA", "TREŠNJA"), "VOĆE"),
                AssociationsColumn(1, fields("CRVENA", "ŽUTA", "PLAVA", "ZELENA"), "BOJE"),
                AssociationsColumn(2, fields("PAS", "MAČKA", "ZEČEVI", "HRČAK"), "KUĆNI LJUBIMCI"),
                AssociationsColumn(3, fields("FUDBAL", "KOŠARKA", "TENIS", "ODBOJKA"), "SPORTOVI")
            ),
            finalSolution = "SLOBODNO VREME"
        )
    )

    private fun fields(vararg words: String) = words.map { AssociationsField(it) }
}
