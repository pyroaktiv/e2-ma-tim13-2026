import { db } from "../db/database";
import { createNotification } from "../routes/notifications";
import { cycleRange, getMeta, previousMonthlyCycleKey, setMeta } from "./cycles";

/**
 * Napredovanje kroz lige (spec 6): liga se određuje iz ukupnog broja zvezda; igrač automatski
 * ulazi/ispada iz lige kada pređe/padne ispod praga. Na kraju mesečnog ciklusa igrač koji se nije
 * plasirao gubi 30% zvezda.
 */

export interface LeagueInfo {
  id: number;
  name: string;
  icon: string;
}

/** league_id najviše lige čiji je prag <= broju zvezda. */
export function leagueIdForStars(stars: number): number {
  const row = db
    .query("SELECT id FROM leagues WHERE min_stars <= ? ORDER BY min_stars DESC LIMIT 1")
    .get(stars) as { id: number } | null;
  return row?.id ?? 1;
}

export function leagueById(id: number): LeagueInfo {
  const row = db.query("SELECT id, name, icon FROM leagues WHERE id = ?").get(id) as LeagueInfo | null;
  return row ?? { id: 1, name: "Bronze", icon: "league_bronze" };
}

/**
 * Preračunava ligu igrača na osnovu ukupnih zvezda; ako se liga promenila, ažurira je i šalje
 * notifikaciju (spec 6.d/6.f). Vraća trenutnu (moguće novu) ligu.
 */
export function applyLeagueForUser(userId: number): LeagueInfo {
  const user = db
    .query("SELECT total_stars, league_id FROM users WHERE id = ?")
    .get(userId) as { total_stars: number; league_id: number } | null;
  if (!user) return leagueById(1);

  const newId = leagueIdForStars(user.total_stars);
  if (newId === user.league_id) return leagueById(user.league_id);

  db.query("UPDATE users SET league_id = ?, updated_at = unixepoch() WHERE id = ?").run(newId, userId);
  const info = leagueById(newId);
  const promoted = newId > user.league_id;
  createNotification(
    userId,
    "RANGIRANJE",
    promoted ? "Napredovanje u ligu" : "Pad u nižu ligu",
    promoted
      ? `Čestitamo! Prešli ste u ligu ${info.name}.`
      : `Pali ste u nižu ligu: ${info.name}.`,
  );
  return info;
}

/**
 * Na prelasku u novi mesečni ciklus, igračima koji se nisu plasirali (nisu odigrali nijednu
 * partiju) u prethodnom ciklusu oduzima 30% zvezda i preračunava ligu (spec 6.e). Idempotentno:
 * svaki ciklus se obrađuje najviše jednom (preko app_meta `penalized_cycle`).
 */
export function processMonthlyRolloverIfDue(): void {
  const prev = previousMonthlyCycleKey();
  if (getMeta("penalized_cycle") === prev) return;

  const activity = db
    .query("SELECT COUNT(*) AS c FROM cycle_scores WHERE period = 'monthly' AND cycle_key = ?")
    .get(prev) as { c: number };

  // Ako prethodni ciklus nije imao nikakve aktivnosti u ovom sistemu, nema koga penalizovati.
  if (activity.c === 0) {
    setMeta("penalized_cycle", prev);
    return;
  }

  const prevEnd = cycleRange("monthly", prev).end; // YYYY-MM-DD
  const prevEndTs = Math.floor(new Date(`${prevEnd}T23:59:59Z`).getTime() / 1000);

  const losers = db
    .query(
      `SELECT u.id AS id, u.total_stars AS total_stars
       FROM users u
       WHERE u.created_at <= ?
         AND NOT EXISTS (
           SELECT 1 FROM cycle_scores cs
           WHERE cs.user_id = u.id AND cs.period = 'monthly' AND cs.cycle_key = ? AND cs.games > 0
         )`,
    )
    .all(prevEndTs, prev) as Array<{ id: number; total_stars: number }>;

  const run = db.transaction(() => {
    for (const u of losers) {
      if (u.total_stars <= 0) continue;
      const newStars = Math.floor(u.total_stars * 0.7);
      db.query("UPDATE users SET total_stars = ?, updated_at = unixepoch() WHERE id = ?").run(newStars, u.id);
      applyLeagueForUser(u.id);
      createNotification(
        u.id,
        "RANGIRANJE",
        "Mesečni ciklus",
        "Niste se plasirali na mesečnoj rang listi — izgubili ste 30% zvezda.",
      );
    }
    setMeta("penalized_cycle", prev);
  });
  run();
}
