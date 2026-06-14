package rs.tim13.slagalica.match

/**
 * Šest igara jedne partije, u redosledu kojim se igraju (spec 3 + redosled iz specifikacije).
 * [key] je identifikator koji se koristi i na backendu (statistika, sadržaj).
 */
enum class MatchGame(val key: String, val displayName: String) {
    KO_ZNA_ZNA("ko_zna_zna", "Ko zna zna"),
    SPOJNICE("spojnice", "Spojnice"),
    ASOCIJACIJE("asocijacije", "Asocijacije"),
    SKOCKO("skocko", "Skočko"),
    KORAK_PO_KORAK("korak_po_korak", "Korak po korak"),
    MOJ_BROJ("moj_broj", "Moj broj");

    val isLast: Boolean get() = ordinal == entries.lastIndex
    fun next(): MatchGame? = entries.getOrNull(ordinal + 1)
}

enum class MatchMode { SOLO, ONLINE }
