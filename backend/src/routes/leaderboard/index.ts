import { db } from "../../db/database";
import { requireAuth } from "../../middleware/auth";
import { json } from "../../util/response";
import { getCurrentWeekBounds } from "../../util/weekly";

interface LeaderboardEntry {
  rank: number;
  user_id: number;
  username: string;
  league_icon: string;
  stars: number;
}

interface LeaderboardResponse {
  cycle_start: string;
  cycle_end: string;
  entries: LeaderboardEntry[];
  my_rank: number | null;
}

function getMyRank(
  userId: number,
  entries: LeaderboardEntry[],
  starsCol: "weekly_stars" | "monthly_stars",
  gamesCol: "weekly_games" | "monthly_games",
): number | null {
  const myEntry = entries.find((e) => e.user_id === userId);
  if (myEntry) return myEntry.rank;

  const played = db
    .query(`SELECT ${gamesCol} AS games FROM users WHERE id = ?`)
    .get(userId) as { games: number } | null;

  if (!played || played.games === 0) return null;

  const rankRow = db
    .query(
      `SELECT COUNT(*) + 1 AS rank
       FROM users
       WHERE ${gamesCol} > 0 AND ${starsCol} > (SELECT ${starsCol} FROM users WHERE id = ?)`,
    )
    .get(userId) as { rank: number } | null;

  return rankRow?.rank ?? null;
}

export async function handleGetWeeklyLeaderboard(req: Request): Promise<Response> {
  const auth = await requireAuth(req);
  if (auth instanceof Response) return auth;

  const rows = db
    .query(
      `SELECT u.id AS user_id, u.username, u.weekly_stars AS stars, l.icon AS league_icon
       FROM users u
       JOIN leagues l ON l.id = u.league_id
       WHERE u.weekly_games > 0
       ORDER BY u.weekly_stars DESC
       LIMIT 100`,
    )
    .all() as Array<{ user_id: number; username: string; stars: number; league_icon: string }>;

  const { start, end } = getCurrentWeekBounds();

  const entries: LeaderboardEntry[] = rows.map((row, index) => ({
    rank:        index + 1,
    user_id:     row.user_id,
    username:    row.username,
    league_icon: row.league_icon,
    stars:       row.stars,
  }));

  const response: LeaderboardResponse = {
    cycle_start: start.toISOString(),
    cycle_end:   end.toISOString(),
    entries,
    my_rank: getMyRank(auth.user_id, entries, "weekly_stars", "weekly_games"),
  };

  return json(response);
}

export async function handleGetMonthlyLeaderboard(req: Request): Promise<Response> {
  const auth = await requireAuth(req);
  if (auth instanceof Response) return auth;

  const rows = db
    .query(
      `SELECT u.id AS user_id, u.username, u.monthly_stars AS stars, l.icon AS league_icon
       FROM users u
       JOIN leagues l ON l.id = u.league_id
       WHERE u.monthly_games > 0
       ORDER BY u.monthly_stars DESC
       LIMIT 100`,
    )
    .all() as Array<{ user_id: number; username: string; stars: number; league_icon: string }>;

  const now = new Date();
  const start = new Date(Date.UTC(now.getUTCFullYear(), now.getUTCMonth(), 1));
  const end = new Date(Date.UTC(now.getUTCFullYear(), now.getUTCMonth() + 1, 0, 23, 59, 59, 999));

  const entries: LeaderboardEntry[] = rows.map((row, index) => ({
    rank:        index + 1,
    user_id:     row.user_id,
    username:    row.username,
    league_icon: row.league_icon,
    stars:       row.stars,
  }));

  const response: LeaderboardResponse = {
    cycle_start: start.toISOString(),
    cycle_end:   end.toISOString(),
    entries,
    my_rank: getMyRank(auth.user_id, entries, "monthly_stars", "monthly_games"),
  };

  return json(response);
}
