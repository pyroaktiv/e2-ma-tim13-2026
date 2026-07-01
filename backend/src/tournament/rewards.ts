import { db } from "../db/database";
import { createNotification } from "../routes/notifications";
import { progressMission } from "../util/missions";
import { pushToUser } from "../util/websocket";
import type { TournamentRewards } from "./types";

function getLeagueName(leagueId: number): string {
  const row = db.query("SELECT name FROM leagues WHERE id = ?").get(leagueId) as { name: string } | null;
  return row?.name ?? "";
}

function updateLeague(userId: number, newStars: number): number {
  const row = db
    .query("SELECT id FROM leagues WHERE min_stars <= ? ORDER BY min_stars DESC LIMIT 1")
    .get(newStars) as { id: number } | null;
  if (row) db.query("UPDATE users SET league_id = ? WHERE id = ?").run(row.id, userId);
  return row?.id ?? 1;
}

function applyStarAndTokenChange(
  userId: number,
  starsDelta: number,
  tokensDelta: number,
): TournamentRewards {
  const user = db
    .query("SELECT total_stars, weekly_stars, monthly_stars, stars_token_progress, tokens, league_id FROM users WHERE id = ?")
    .get(userId) as {
      total_stars: number;
      weekly_stars: number;
      monthly_stars: number;
      stars_token_progress: number;
      tokens: number;
      league_id: number;
    } | null;

  if (!user) {
    return { starsDelta: 0, tokensDelta: 0, totalStars: 0, tokens: 0, league: "" };
  }

  const newTotal = Math.max(0, user.total_stars + starsDelta);
  const effectiveDelta = newTotal - user.total_stars;

  // Only positive star changes count toward the 50-star token accumulator.
  const earned = Math.max(0, starsDelta);
  let progress = user.stars_token_progress + earned;
  let tokensFromStars = 0;
  while (progress >= 50) {
    tokensFromStars++;
    progress -= 50;
  }
  const totalTokensDelta = tokensDelta + tokensFromStars;
  const newTokens = user.tokens + totalTokensDelta;

  db.query(`
    UPDATE users
    SET total_stars          = ?,
        weekly_stars         = weekly_stars  + ?,
        monthly_stars        = monthly_stars + ?,
        stars_token_progress = ?,
        tokens               = ?,
        updated_at           = unixepoch()
    WHERE id = ?
  `).run(newTotal, effectiveDelta, effectiveDelta, progress, newTokens, userId);

  const newLeagueId = updateLeague(userId, newTotal);

  return {
    starsDelta: effectiveDelta,
    tokensDelta: totalTokensDelta,
    totalStars: newTotal,
    tokens: newTokens,
    league: getLeagueName(newLeagueId),
  };
}

export function applySemiWinRewards(userId: number, myScore: number): TournamentRewards {
  const base = Math.floor(myScore / 40);
  const starsDelta = 10 + base;
  const rewards = applyStarAndTokenChange(userId, starsDelta, 2);
  pushToUser(userId, { type: "tournament_rewards", phase: "semi", won: true, rewards });
  return rewards;
}

export function applySemiLossRewards(_userId: number): null {
  return null;
}

export function applyFinalWinRewards(userId: number, myScore: number): TournamentRewards {
  const base = Math.floor(myScore / 40);
  // +10 regular win + 10 bonus = 20 base
  const starsDelta = 20 + base;
  const rewards = applyStarAndTokenChange(userId, starsDelta, 3);

  progressMission(userId, "win_tournament");

  createNotification(
    userId,
    "NAGRADE",
    "Pobeda u turniru!",
    `Pobedili ste turnir i dobili ${rewards.tokensDelta} žetona i ${rewards.starsDelta} zvezda.`,
  );
  pushToUser(userId, { type: "tournament_rewards", phase: "final", won: true, rewards });
  return rewards;
}

export function applyFinalLossRewards(userId: number, myScore: number): TournamentRewards {
  const base = Math.floor(myScore / 40);
  const starsDelta = -10 + base;
  const rewards = applyStarAndTokenChange(userId, starsDelta, 0);
  pushToUser(userId, { type: "tournament_rewards", phase: "final", won: false, rewards });
  return rewards;
}
