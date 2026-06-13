package rs.tim13.slagalica.koznazna.data

import rs.tim13.slagalica.koznazna.model.KoZnaZnaQuestion

class MockKoZnaZnaGameRepository : KoZnaZnaGameRepository {

    override fun getQuestions(): List<KoZnaZnaQuestion> = listOf(
        KoZnaZnaQuestion(
            "Koji glumac tumači lik Popa u filmu 'Munje!'?",
            listOf("Nikola Đuričko", "Sergej Trifunović", "Boris Milivojević", "Nenad Jezdić"),
            correctIndex = 1
        ),
        KoZnaZnaQuestion(
            "Koji je glavni grad Australije?",
            listOf("Sidnej", "Melburn", "Kanbera", "Pert"),
            correctIndex = 2
        ),
        KoZnaZnaQuestion(
            "Koja planeta je najbliža Suncu?",
            listOf("Venera", "Zemlja", "Mars", "Merkur"),
            correctIndex = 3
        ),
        KoZnaZnaQuestion(
            "Ko je napisao roman 'Na Drini ćuprija'?",
            listOf("Miloš Crnjanski", "Ivo Andrić", "Meša Selimović", "Dobrica Ćosić"),
            correctIndex = 1
        ),
        KoZnaZnaQuestion(
            "Koja je najduža reka u Evropi?",
            listOf("Dunav", "Volga", "Sava", "Rajna"),
            correctIndex = 1
        )
    )
}
