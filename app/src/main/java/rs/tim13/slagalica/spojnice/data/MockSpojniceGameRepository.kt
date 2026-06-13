package rs.tim13.slagalica.spojnice.data

import rs.tim13.slagalica.spojnice.model.SpojniceRound

class MockSpojniceGameRepository : SpojniceGameRepository {

    override fun getRounds(): List<SpojniceRound> = listOf(
        SpojniceRound(
            leftItems = listOf("Pop", "Mare", "Gojko Šiša", "Kata", "Policajac"),
            rightItems = listOf(
                "Sergej Trifunović", "Nebojša Glogovac", "Nikola Đuričko",
                "Maja Mandžuka", "Boris Milivojević"
            ),
            solution = mapOf(0 to 0, 1 to 4, 2 to 2, 3 to 3, 4 to 1)
        ),
        SpojniceRound(
            leftItems = listOf("Kengur", "Braca", "Šomi", "Iris", "Živac"),
            rightItems = listOf(
                "Marija Karan", "Nebojša Glogovac", "Boris Milivojević",
                "Nikola Đuričko", "Sergej Trifunović"
            ),
            solution = mapOf(0 to 3, 1 to 4, 2 to 2, 3 to 0, 4 to 1)
        )
    )
}
