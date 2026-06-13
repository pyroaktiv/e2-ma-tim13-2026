// Persists the outcome of a finished real-time match: per-game statistics,
// the win/loss summary, awarded stars and league progression. This is what
// makes the Profile screen (also Student 2) show real data after a game.

import { db } from "../db/database";
import type { GameType } from "../model/stats";

function mergeGameStats(
  userId: number,
  game: GameType,
  patch: Record<string, number>,
): void {
  const row = db
    .query("SELECT stats_json FROM game_stats WHERE user_id = ? AND game = ?")
    .get(userId, game) as { stats_json: string } | null;

  const current: Record<string, number> = row ? JSON.parse(row.stats_json) : {};
  for (const [key, value] of Object.entries(patch)) {
    current[key] = (current[key] ?? 0) + value;
  }

  db.query(
    `INSERT INTO game_stats (user_id, game, stats_json, updated_at)
     VALUES (?, ?, ?, unixepoch())
     ON CONFLICT(user_id, game)
     DO UPDATE SET stats_json = excluded.stats_json, updated_at = unixepoch()`,
  ).run(userId, game, JSON.stringify(current));
}

export function recordKoZnaZna(
  userId: number,
  correct: number,
  missed: number,
): void {
  mergeGameStats(userId, "ko_zna_zna", { correct, missed });
}

export function recordSpojnice(
  userId: number,
  successful: number,
  total: number,
): void {
  mergeGameStats(userId, "spojnice", { successful, total });
}

export function recordMojBroj(
  userId: number,
  totalAttempts: number,
  exactHits: number,
): void {
  mergeGameStats(userId, "moj_broj", {
    total_attempts: totalAttempts,
    exact_hits: exactHits,
  });
}

// Accumulates points scored and number of plays for a game, so the profile can
// show the average points earned per game (spec FR2.c.i).
export function recordGamePoints(
  userId: number,
  game: GameType,
  points: number,
): void {
  mergeGameStats(userId, game, { points_sum: points, plays: 1 });
}

export function recordAsocijacije(
  userId: number,
  solved: number,
  unsolved: number,
): void {
  mergeGameStats(userId, "asocijacije", { solved, unsolved });
}

// Read-modify-write for an array stat field (adds element-wise).
function mergeArrayStat(
  userId: number,
  game: GameType,
  key: string,
  delta: number[],
  scalars: Record<string, number> = {},
): void {
  const row = db
    .query("SELECT stats_json FROM game_stats WHERE user_id = ? AND game = ?")
    .get(userId, game) as { stats_json: string } | null;
  const current: Record<string, unknown> = row ? JSON.parse(row.stats_json) : {};

  const existing = Array.isArray(current[key]) ? (current[key] as number[]) : [];
  const merged = delta.map((d, i) => (existing[i] ?? 0) + d);
  current[key] = merged;
  for (const [k, v] of Object.entries(scalars)) {
    current[k] = ((current[k] as number) ?? 0) + v;
  }

  db.query(
    `INSERT INTO game_stats (user_id, game, stats_json, updated_at)
     VALUES (?, ?, ?, unixepoch())
     ON CONFLICT(user_id, game)
     DO UPDATE SET stats_json = excluded.stats_json, updated_at = unixepoch()`,
  ).run(userId, game, JSON.stringify(current));
}

export function recordSkocko(
  userId: number,
  correctAtAttempt: number[],
  failed: number,
): void {
  mergeArrayStat(userId, "skocko", "correct_at_attempt", correctAtAttempt, {
    failed,
  });
}

export function recordKorakPoKorak(userId: number, guessedAtStep: number[]): void {
  mergeArrayStat(userId, "korak_po_korak", "guessed_at_step", guessedAtStep);
}

function applyStars(userId: number, delta: number): number {
  const row = db
    .query("SELECT total_stars FROM users WHERE id = ?")
    .get(userId) as { total_stars: number } | null;
  const current = row?.total_stars ?? 0;
  const next = Math.max(0, current + delta);

  const league = db
    .query(
      "SELECT id FROM leagues WHERE min_stars <= ? ORDER BY min_stars DESC LIMIT 1",
    )
    .get(next) as { id: number } | null;

  db.query(
    "UPDATE users SET total_stars = ?, league_id = ?, updated_at = unixepoch() WHERE id = ?",
  ).run(next, league?.id ?? 1, userId);

  return next;
}

function bumpSummary(userId: number, outcome: Outcome): void {
  db.query("INSERT OR IGNORE INTO match_summary (user_id) VALUES (?)").run(
    userId,
  );
  db.query(
    `UPDATE match_summary
     SET total_games = total_games + 1,
         wins  = wins  + ?,
         losses = losses + ?,
         updated_at = unixepoch()
     WHERE user_id = ?`,
  ).run(outcome === "win" ? 1 : 0, outcome === "loss" ? 1 : 0, userId);
}

export type Outcome = "win" | "loss" | "tie";

export interface MatchResult {
  userId: number;
  points: number;
  outcome: Outcome;
}

// Stars per spec: winner +10 + floor(points/40); loser -10 + floor(points/40),
// never dropping below 0. On a tie nobody gains/loses the base 10, only the
// per-40-points bonus applies. Returns the star delta actually applied.
export function finishMatch(
  a: MatchResult,
  b: MatchResult,
): Record<number, number> {
  const deltas: Record<number, number> = {};
  const tx = db.transaction(() => {
    for (const r of [a, b]) {
      const bonus = Math.floor(r.points / 40);
      const base = r.outcome === "win" ? 10 : r.outcome === "loss" ? -10 : 0;
      const before = (
        db.query("SELECT total_stars FROM users WHERE id = ?").get(r.userId) as
          | { total_stars: number }
          | null
      )?.total_stars ?? 0;
      const after = applyStars(r.userId, base + bonus);
      deltas[r.userId] = after - before;
      bumpSummary(r.userId, r.outcome);
    }
  });
  tx();
  return deltas;
}
