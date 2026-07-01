package rs.tim13.slagalica.korakpokorak.data

import rs.tim13.slagalica.korakpokorak.model.KorakPoKorakRound

class MockKorakPoKorakGameRepository : KorakPoKorakGameRepository {

    override fun getRounds(): List<KorakPoKorakRound> = listOf(
        KorakPoKorakRound(
            clues = listOf(
                "Može biti bela, crna ili mlečna",
                "Često se poklanja za praznike",
                "Sadrži kakao i maslac",
                "Švajcarska je poznata po njoj",
                "Može biti sa lešnicima",
                "Topi se na suncu",
                "Slatkiš u kockicama"
            ),
            solution = "ČOKOLADA"
        ),
        KorakPoKorakRound(
            clues = listOf(
                "Najveća je mačka na svetu",
                "Ima prepoznatljive pruge",
                "Odličan je plivač",
                "Živi u azijskim džunglama",
                "Simbol je snage i moći",
                "U Sibiru dostiže najveću veličinu",
                "Šir Kan je jedan od njih"
            ),
            solution = "TIGAR"
        )
    )
}
