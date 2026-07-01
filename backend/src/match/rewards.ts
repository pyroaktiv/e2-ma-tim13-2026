import { db } from "../db/database";
import { createNotification } from "../routes/notifications";
import type { MatchRewards } from "./types";

/**
 * Primenjuje ishod rangirane partije na registrovanog igrača (spec 3.d):
 * pobednik +10★ + 1★ za svakih 40 poena, gubitnik -10★ + 1★/40 (ukupno ne ispod 0),
 * a svakih 50 zarađenih zvezda donosi 1 token. Zvezde ulaze i u nedeljni/mesečni ciklus
 * (rang liste i regioni — spec 4/5), i preračunava se liga (spec 6.d).
 */
export function applyMatchOutcome(userId: number, myScore: number, oppScore: number): MatchRewards {
  const user = db
    .query(
      "SELECT total_stars, weekly_stars, monthly_stars, stars_token_progress, tokens, league_id FROM users WHERE id = ?",
    )
    .get(userId) as
    | {
        total_stars: number;
        weekly_stars: number;
        monthly_stars: number;
        stars_token_progress: number;
        tokens: number;
        league_id: number;
      }
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
  const newWeekly = Math.max(0, user.weekly_stars + delta);
  const newMonthly = Math.max(0, user.monthly_stars + delta);

  // Ka tokenu se broje samo zarađene zvezde (osnova + bonus za pobedu), ne i gubitak.
  const earned = base + (outcome === "win" ? 10 : 0);
  let progress = user.stars_token_progress + earned;
  let tokensGranted = 0;
  while (progress >= 50) {
    tokensGranted++;
    progress -= 50;
  }
  const newTokens = user.tokens + tokensGranted;

  // Spec 4/5: zvezde i partija ulaze u tekući nedeljni/mesečni ciklus (rang liste, regioni).
  db.query(
    `UPDATE users
       SET total_stars   = ?,
           weekly_stars  = ?,
           monthly_stars = ?,
           weekly_games  = weekly_games  + 1,
           monthly_games = monthly_games + 1,
           stars_token_progress = ?,
           tokens        = ?,
           updated_at    = unixepoch()
     WHERE id = ?`,
  ).run(newStars, newWeekly, newMonthly, progress, newTokens, userId);

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

  // Spec 6.d: preračunaj ligu na osnovu novog broja zvezda; notifikacija samo na promenu.
  const newLeague = db
    .query("SELECT id, name, icon FROM leagues WHERE min_stars <= ? ORDER BY min_stars DESC LIMIT 1")
    .get(newStars) as { id: number; name: string; icon: string } | null;

  let leagueName = "";
  if (newLeague) {
    leagueName = newLeague.name;
    if (newLeague.id !== user.league_id) {
      db.query("UPDATE users SET league_id = ? WHERE id = ?").run(newLeague.id, userId);
      const promoted = newLeague.id > user.league_id;
      createNotification(
        userId,
        "RANGIRANJE",
        promoted ? `Napredovanje u ligu: ${newLeague.name}` : `Pad u nižu ligu: ${newLeague.name}`,
        promoted
          ? `Čestitamo! Prešli ste u ligu ${newLeague.name}.`
          : `Pali ste u nižu ligu: ${newLeague.name}.`,
      );
    }
  }

  return {
    won: outcome === "win",
    starsDelta: effectiveDelta,
    tokensDelta: tokensGranted,
    totalStars: newStars,
    tokens: newTokens,
    league: leagueName,
  };
}
