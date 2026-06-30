import { db } from "../db/database";

/**
 * Samostalni nedeljni/mesečni rang ciklusi (spec 4/5/6) — nezavisno od tačke 4 (kolega 3).
 * Beleži zvezde i broj partija po igraču u tekućem ciklusu; istorija ciklusa ostaje sačuvana
 * za „prethodni ciklus" (region top-3) i statistiku regiona.
 */

export type CyclePeriod = "weekly" | "monthly";

function pad2(n: number): string {
  return String(n).padStart(2, "0");
}

function isoDate(d: Date): string {
  return `${d.getUTCFullYear()}-${pad2(d.getUTCMonth() + 1)}-${pad2(d.getUTCDate())}`;
}

/** ISO broj nedelje (npr. „2026-W26"). */
export function weeklyCycleKey(d = new Date()): string {
  const date = new Date(Date.UTC(d.getFullYear(), d.getMonth(), d.getDate()));
  const dayNum = (date.getUTCDay() + 6) % 7; // ponedeljak = 0
  date.setUTCDate(date.getUTCDate() - dayNum + 3); // pomeri na četvrtak te nedelje
  const firstThursday = new Date(Date.UTC(date.getUTCFullYear(), 0, 4));
  const week =
    1 +
    Math.round(
      ((date.getTime() - firstThursday.getTime()) / 86400000 -
        3 +
        ((firstThursday.getUTCDay() + 6) % 7)) /
        7,
    );
  return `${date.getUTCFullYear()}-W${pad2(week)}`;
}

/** Mesečni ključ „YYYY-MM". */
export function monthlyCycleKey(d = new Date()): string {
  return `${d.getFullYear()}-${pad2(d.getMonth() + 1)}`;
}

export function previousMonthlyCycleKey(d = new Date()): string {
  return monthlyCycleKey(new Date(d.getFullYear(), d.getMonth() - 1, 1));
}

export function cycleKey(period: CyclePeriod, d = new Date()): string {
  return period === "weekly" ? weeklyCycleKey(d) : monthlyCycleKey(d);
}

/** Opseg datuma ciklusa za prikaz (spec 4.e). */
export function cycleRange(period: CyclePeriod, key = cycleKey(period)): { start: string; end: string } {
  if (period === "monthly") {
    const [y, m] = key.split("-").map(Number);
    return { start: isoDate(new Date(Date.UTC(y, m - 1, 1))), end: isoDate(new Date(Date.UTC(y, m, 0))) };
  }
  const [yStr, wStr] = key.split("-W");
  const y = Number(yStr);
  const w = Number(wStr);
  // Ponedeljak ISO nedelje w u godini y.
  const simple = new Date(Date.UTC(y, 0, 1 + (w - 1) * 7));
  const dow = simple.getUTCDay();
  const monday = new Date(simple);
  if (dow <= 4) monday.setUTCDate(simple.getUTCDate() - ((dow + 6) % 7));
  else monday.setUTCDate(simple.getUTCDate() + (8 - dow));
  const sunday = new Date(monday);
  sunday.setUTCDate(monday.getUTCDate() + 6);
  return { start: isoDate(monday), end: isoDate(sunday) };
}

/** Beleži ishod rangirane partije igraču u tekućem nedeljnom i mesečnom ciklusu. */
export function recordCycleResult(userId: number, starsEarned: number): void {
  const gained = Math.max(0, Math.trunc(starsEarned));
  const stmt = db.prepare(
    `INSERT INTO cycle_scores (user_id, period, cycle_key, stars, games)
     VALUES (?, ?, ?, ?, 1)
     ON CONFLICT(user_id, period, cycle_key)
     DO UPDATE SET stars = stars + ?, games = games + 1`,
  );
  stmt.run(userId, "weekly", weeklyCycleKey(), gained, gained);
  stmt.run(userId, "monthly", monthlyCycleKey(), gained, gained);
}

/** Plasman igrača na listi datog perioda (spec 4.a: rangiran samo ako je odigrao bar jednu partiju). */
export function getRank(userId: number, period: CyclePeriod, key = cycleKey(period)): number | null {
  const row = db
    .query("SELECT stars, games FROM cycle_scores WHERE user_id = ? AND period = ? AND cycle_key = ?")
    .get(userId, period, key) as { stars: number; games: number } | null;
  if (!row || row.games <= 0) return null;

  const higher = db
    .query(
      "SELECT COUNT(*) AS c FROM cycle_scores WHERE period = ? AND cycle_key = ? AND games > 0 AND stars > ?",
    )
    .get(period, key, row.stars) as { c: number };
  return higher.c + 1;
}

export interface LeaderboardEntry {
  userId: number;
  username: string;
  avatar: string;
  region: string;
  leagueIcon: string;
  stars: number;
  rank: number;
}

/** Rang lista perioda: igrači sa bar jednom partijom u ciklusu, sortirani po osvojenim zvezdama. */
export function getLeaderboard(period: CyclePeriod, limit = 50, key = cycleKey(period)): LeaderboardEntry[] {
  const rows = db
    .query(
      `SELECT u.id AS userId, u.username, u.avatar, u.region, l.icon AS leagueIcon, cs.stars
       FROM cycle_scores cs
       JOIN users u ON u.id = cs.user_id
       JOIN leagues l ON l.id = u.league_id
       WHERE cs.period = ? AND cs.cycle_key = ? AND cs.games > 0
       ORDER BY cs.stars DESC, u.username ASC
       LIMIT ?`,
    )
    .all(period, key, limit) as Omit<LeaderboardEntry, "rank">[];
  return rows.map((r, i) => ({ ...r, rank: i + 1 }));
}

export interface RegionStars {
  region: string;
  stars: number;
}

/** Ukupne zvezde po regionu u datom mesečnom ciklusu (spec 5.b). */
export function getRegionMonthlyStars(key = monthlyCycleKey()): RegionStars[] {
  return db
    .query(
      `SELECT u.region AS region, COALESCE(SUM(cs.stars), 0) AS stars
       FROM users u
       LEFT JOIN cycle_scores cs
         ON cs.user_id = u.id AND cs.period = 'monthly' AND cs.cycle_key = ?
       GROUP BY u.region
       ORDER BY stars DESC, u.region ASC`,
    )
    .all(key) as RegionStars[];
}

/** Top 3 regiona prethodnog mesečnog ciklusa (spec 5.e — boja okvira avatara). */
export function getPreviousRegionTop3(): string[] {
  const key = previousMonthlyCycleKey();
  const rows = db
    .query(
      `SELECT u.region AS region, SUM(cs.stars) AS stars
       FROM cycle_scores cs
       JOIN users u ON u.id = cs.user_id
       WHERE cs.period = 'monthly' AND cs.cycle_key = ?
       GROUP BY u.region
       HAVING stars > 0
       ORDER BY stars DESC
       LIMIT 3`,
    )
    .all(key) as Array<{ region: string; stars: number }>;
  return rows.map((r) => r.region);
}

export interface RegionMedals {
  first: number;
  second: number;
  third: number;
}

/** Broj osvojenih 1./2./3. mesta po regionu kroz sve ZAVRŠENE mesečne cikluse (spec 5.d). */
export function getRegionMedalCounts(): Record<string, RegionMedals> {
  const current = monthlyCycleKey();
  const cycles = db
    .query("SELECT DISTINCT cycle_key FROM cycle_scores WHERE period = 'monthly'")
    .all() as Array<{ cycle_key: string }>;

  const result: Record<string, RegionMedals> = {};
  for (const { cycle_key } of cycles) {
    if (cycle_key === current) continue;
    const ranking = db
      .query(
        `SELECT u.region AS region, SUM(cs.stars) AS stars
         FROM cycle_scores cs
         JOIN users u ON u.id = cs.user_id
         WHERE cs.period = 'monthly' AND cs.cycle_key = ?
         GROUP BY u.region
         HAVING stars > 0
         ORDER BY stars DESC`,
      )
      .all(cycle_key) as Array<{ region: string; stars: number }>;

    ranking.forEach((r, i) => {
      const slot = (result[r.region] ??= { first: 0, second: 0, third: 0 });
      if (i === 0) slot.first++;
      else if (i === 1) slot.second++;
      else if (i === 2) slot.third++;
    });
  }
  return result;
}

/** app_meta helper-i (npr. poslednji obrađeni mesečni ciklus za penale lige). */
export function getMeta(key: string): string | null {
  const row = db.query("SELECT value FROM app_meta WHERE key = ?").get(key) as { value: string } | null;
  return row?.value ?? null;
}

export function setMeta(key: string, value: string): void {
  db.query(
    "INSERT INTO app_meta (key, value) VALUES (?, ?) ON CONFLICT(key) DO UPDATE SET value = excluded.value",
  ).run(key, value);
}
