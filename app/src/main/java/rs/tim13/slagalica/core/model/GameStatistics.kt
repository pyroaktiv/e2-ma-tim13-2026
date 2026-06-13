package rs.tim13.slagalica.core.model

/**
 * Marker za podatke koje pojedinačna igra prijavljuje na kraju (spec 2.c).
 *
 * Svaka igra definiše sopstvenu implementaciju sa svojim brojačima (po igraču),
 * npr. odnos pogođenih/promašenih pitanja, procenat po pokušaju, itd.
 *
 * Koordinator partije (3. Igranje partija) / izazov (9) / turnir (10) sakuplja
 * ove vrednosti kroz [GameResult] i prosleđuje ih sloju za statistiku profila.
 */
interface GameStatistics
