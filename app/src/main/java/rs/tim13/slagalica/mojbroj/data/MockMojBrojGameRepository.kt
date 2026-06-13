package rs.tim13.slagalica.mojbroj.data

import rs.tim13.slagalica.mojbroj.model.MojBrojRound
import kotlin.random.Random

/**
 * Generiše nasumične zagonetke „Moj broj": traženi trocifreni broj i šest brojeva
 * (četiri jednocifrena, jedan iz {10,15,20} i jedan iz {25,50,75,100}).
 */
class MockMojBrojGameRepository : MojBrojGameRepository {

    override fun getRounds(): List<MojBrojRound> = List(ROUND_COUNT) { randomRound() }

    private fun randomRound(): MojBrojRound {
        val numbers = buildList {
            repeat(4) { add(Random.nextInt(1, 10)) }
            add(listOf(10, 15, 20).random())
            add(listOf(25, 50, 75, 100).random())
        }
        return MojBrojRound(target = Random.nextInt(100, 1000), numbers = numbers)
    }

    companion object {
        private const val ROUND_COUNT = 2
    }
}
