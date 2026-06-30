import { db } from "../db/database";
import { recordCycleResult } from "../ranking/cycles";
import { applyLeagueForUser } from "../ranking/leagues";
import type { MatchRewards } from "./types";

/**
 * Primenjuje ishod partije na registrovanog igrača (spec 3.d):
 * pobednik +10★ + 1★ za svakih 40 poena, gubitnik -10★ + 1★/40 (ukupno ne ispod 0),
 * a svakih 50 zarađenih zvezda donosi 1 token. Nakon promene zvezda preračunava ligu (spec 6.d)
 * i beleži ciklus za rang liste (spec 4/5).
 */
export function applyMatchOutcome(userId: number, myScore: number, oppScore: number): MatchRewards {
  const user = db
    .query("SELECT total_stars, stars_token_progress, tokens, league_id FROM users WHERE id = ?")
    .get(userId) as
    | { total_stars: number; stars_token_progress: number; tokens: number; league_id: number }
    | null;

  if (!user) {
    return { won: false, starsDelta: 0, tokensDelta: 0, totalStars: 0, tokens: 0, league: "" };
  }

  const outcome = myScore > oppScore ? "win" : myScore < oppScore ? "loss" : "draw";
  const base = Math.floor(myScore / 40);
  let delta = base;
  if (outcome === "win") delta += 10;
  else if (outcome === "loss") delta -= 10;

  const newStars = Math.max(0, user.total_stars + delta);
  const effectiveDelta = newStars - user.total_stars;

  // Ka tokenu se broje samo zarađene zvezde (osnova + bonus za pobedu), ne i gubitak.
  const earned = base + (outcome === "win" ? 10 : 0);
  let progress = user.stars_token_progress + earned;
  let tokensGranted = 0;
  while (progress >= 50) {
    tokensGranted++;
    progress -= 50;
  }
  const newTokens = user.tokens + tokensGranted;

  db.query(
    "UPDATE users SET total_stars = ?, stars_token_progress = ?, tokens = ?, updated_at = unixepoch() WHERE id = ?",
  ).run(newStars, progress, newTokens, userId);

  // Zvezde i partija ulaze u tekući nedeljni/mesečni rang ciklus (spec 4/5).
  recordCycleResult(userId, earned);

  db.query(
    `INSERT INTO match_summary (user_id, total_games, wins, losses)
     VALUES (?, 1, ?, ?)
     ON CONFLICT(user_id) DO UPDATE SET
       total_games = total_games + 1,
       wins = wins + ?,
       losses = losses + ?,
       updated_at = unixepoch()`,
  ).run(
    userId,
    outcome === "win" ? 1 : 0,
    outcome === "loss" ? 1 : 0,
    outcome === "win" ? 1 : 0,
    outcome === "loss" ? 1 : 0,
  );

  // Preračunaj ligu na osnovu novog broja zvezda (spec 6.d) i vrati njen naziv za prikaz.
  const league = applyLeagueForUser(userId);

  return {
    won: outcome === "win",
    starsDelta: effectiveDelta,
    tokensDelta: tokensGranted,
    totalStars: newStars,
    tokens: newTokens,
    league: league.name,
  };
}
