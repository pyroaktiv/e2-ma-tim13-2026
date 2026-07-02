import { db } from "../db/database";
import type { PerGameStats } from "./types";

/**
 * Perzistencija per-game statistike (spec 2.c). Klijent na kraju partije šalje statistiku
 * svake od šest igara sa vrednostima za OBA igrača (camelCase, npr. `blueCorrect`/`redCorrect`).
 * Ovde se „plavi" doprinos pripisuje plavom igraču, „crveni" crvenom, i akumulira u game_stats
 * u formatu koji čita ruta GET /api/user/stats ([model/stats.ts]).
 */

type Side = "blue" | "red";
type Increment = Record<string, number | number[]>;

/** Podrazumevani (nulti) oblik statistike po igri — mora da prati [model/stats.ts]. */
const DEFAULTS: Record<string, Increment> = {
  ko_zna_zna: { correct: 0, missed: 0 },
  moj_broj: { total_attempts: 0, exact_hits: 0 },
  korak_po_korak: { guessed_at_step: [0, 0, 0, 0, 0, 0, 0], failed: 0 },
  asocijacije: { solved: 0, unsolved: 0 },
  skocko: { correct_at_attempt: [0, 0, 0, 0, 0, 0], failed: 0 },
  spojnice: { total: 0, successful: 0 },
};

function num(stats: Record<string, unknown>, key: string): number {
  const v = stats[key];
  return typeof v === "number" && Number.isFinite(v) ? v : 0;
}

function arr(stats: Record<string, unknown>, key: string): number[] {
  const v = stats[key];
  return Array.isArray(v) ? v.map((x) => (typeof x === "number" && Number.isFinite(x) ? x : 0)) : [];
}

/** Izvlači doprinos jedne strane iz žičane statistike u backend format date igre. */
function increment(game: string, stats: Record<string, unknown>, side: Side): Increment | null {
  const p: Side = side;
  switch (game) {
    case "ko_zna_zna":
      return { correct: num(stats, `${p}Correct`), missed: num(stats, `${p}Wrong`) };
    case "spojnice":
      return { total: num(stats, `${p}Attempts`), successful: num(stats, `${p}Connected`) };
    case "moj_broj":
      return { total_attempts: num(stats, `${p}Played`), exact_hits: num(stats, `${p}Found`) };
    case "asocijacije":
      return { solved: num(stats, `${p}SolvedCount`), unsolved: num(stats, `${p}UnsolvedCount`) };
    case "korak_po_korak":
      return { guessed_at_step: arr(stats, `${p}SolvedAtStep`), failed: num(stats, `${p}Failed`) };
    case "skocko":
      return { correct_at_attempt: arr(stats, `${p}CorrectAtAttempt`), failed: num(stats, `${p}Failed`) };
    default:
      return null;
  }
}

/** Sabira inkrement na bazno stanje: skalari se sabiraju, nizovi po elementima. */
function mergeInto(base: Increment, inc: Increment): Increment {
  const out: Increment = { ...base };
  for (const [key, value] of Object.entries(inc)) {
    if (Array.isArray(value)) {
      const current = Array.isArray(out[key]) ? (out[key] as number[]) : [];
      const len = Math.max(current.length, value.length);
      out[key] = Array.from({ length: len }, (_, i) => (current[i] ?? 0) + (value[i] ?? 0));
    } else {
      out[key] = ((out[key] as number) ?? 0) + value;
    }
  }
  return out;
}

function applyUserGame(userId: number, game: string, inc: Increment): void {
  const row = db
    .query("SELECT stats_json FROM game_stats WHERE user_id = ? AND game = ?")
    .get(userId, game) as { stats_json: string } | null;

  const stored = row ? (JSON.parse(row.stats_json) as Increment) : {};
  const base: Increment = { ...DEFAULTS[game], ...stored };
  const merged = mergeInto(base, inc);

  db.query(
    `INSERT INTO game_stats (user_id, game, stats_json, updated_at)
     VALUES (?, ?, ?, unixepoch())
     ON CONFLICT(user_id, game) DO UPDATE SET stats_json = excluded.stats_json, updated_at = unixepoch()`,
  ).run(userId, game, JSON.stringify(merged));
}

/**
 * Upisuje per-game statistiku partije u game_stats. Plavi doprinos ide plavom registrovanom
 * igraču, crveni crvenom; gosti (userId === null) se preskaču. Za izazov (jedan igrač po
 * rezultatu) prosledi njegov id kao `blueUserId` i `null` kao `redUserId`.
 */
export function applyPerGameStats(
  blueUserId: number | null,
  redUserId: number | null,
  perGame: PerGameStats[] | undefined,
): void {
  if (!perGame?.length) return;
  if (blueUserId === null && redUserId === null) return;

  const run = db.transaction(() => {
    for (const entry of perGame) {
      if (!entry || typeof entry.game !== "string" || !(entry.game in DEFAULTS)) continue;
      const stats = (entry.statistics ?? {}) as Record<string, unknown>;
      if (blueUserId !== null) {
        const inc = increment(entry.game, stats, "blue");
        if (inc) applyUserGame(blueUserId, entry.game, inc);
      }
      if (redUserId !== null) {
        const inc = increment(entry.game, stats, "red");
        if (inc) applyUserGame(redUserId, entry.game, inc);
      }
    }
  });
  run();
}
