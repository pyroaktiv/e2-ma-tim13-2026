package rs.tim13.slagalica.match

/**
 * Šest igara jedne partije, u redosledu kojim se igraju (spec 3 + redosled iz specifikacije).
 * [key] je identifikator koji se koristi i na backendu (statistika, sadržaj).
 */
enum class MatchGame(val key: String, val displayName: String) {
    SPOJNICE("spojnice", "Spojnice"),
    MOJ_BROJ("moj_broj", "Moj broj"),
    KORAK_PO_KORAK("korak_po_korak", "Korak po korak"),
    SKOCKO("skocko", "Skočko"),
    KO_ZNA_ZNA("ko_zna_zna", "Ko zna zna"),
    ASOCIJACIJE("asocijacije", "Asocijacije");

    val isLast: Boolean get() = ordinal == entries.lastIndex
    fun next(): MatchGame? = entries.getOrNull(ordinal + 1)
}

/**
 * CHALLENGE: izazov (spec 9) — partija se igra samostalno, sadržaj dolazi sa servera.
 * FRIEND: prijateljska partija (spec 7.d) — server je već spojio dva prijatelja i poslao
 * `match_found`; klijent ne traži protivnika, samo uđe u partiju (nerangirana).
 * TOURNAMENT_SEMI/FINAL: turnir (spec 10).
 */
enum class MatchMode { SOLO, ONLINE, CHALLENGE, FRIEND, TOURNAMENT_SEMI, TOURNAMENT_FINAL }
