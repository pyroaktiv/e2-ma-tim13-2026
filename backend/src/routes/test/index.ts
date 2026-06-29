import { db } from "../../db/database";
import { json } from "../../util/response";
import { checkAndRunWeeklyReset } from "../../util/weekly";
import { checkAndRunMonthlyReset } from "../../util/monthly";
import { progressMission } from "../../util/missions";
import type { MissionKey } from "../../model/mission";

export async function handleForceWeeklyReset(_req: Request): Promise<Response> {
  db.query("UPDATE server_config SET value = '2000-01' WHERE key = 'last_weekly_reset'").run();
  checkAndRunWeeklyReset();
  return json({ ok: true, message: "Weekly reset triggered" });
}

export async function handleForceMonthlyReset(_req: Request): Promise<Response> {
  db.query("UPDATE server_config SET value = '2000-01' WHERE key = 'last_monthly_reset'").run();
  checkAndRunMonthlyReset();
  return json({ ok: true, message: "Monthly reset triggered" });
}

const TEST_USER_STATS = [
  { username: "alice",  tokens: 10, weeklyStars: 100, monthlyStars: 150, weeklyGames: 5, monthlyGames: 8 },
  { username: "bob",    tokens: 5,  weeklyStars: 0,   monthlyStars: 0,   weeklyGames: 0, monthlyGames: 0 },
  { username: "user3",  tokens: 5,  weeklyStars: 95,  monthlyStars: 140, weeklyGames: 4, monthlyGames: 7 },
  { username: "user4",  tokens: 5,  weeklyStars: 85,  monthlyStars: 130, weeklyGames: 4, monthlyGames: 6 },
  { username: "user5",  tokens: 5,  weeklyStars: 75,  monthlyStars: 120, weeklyGames: 3, monthlyGames: 5 },
  { username: "user6",  tokens: 5,  weeklyStars: 65,  monthlyStars: 110, weeklyGames: 3, monthlyGames: 5 },
  { username: "user7",  tokens: 5,  weeklyStars: 55,  monthlyStars: 100, weeklyGames: 2, monthlyGames: 4 },
  { username: "user8",  tokens: 5,  weeklyStars: 45,  monthlyStars: 90,  weeklyGames: 2, monthlyGames: 4 },
  { username: "user9",  tokens: 5,  weeklyStars: 35,  monthlyStars: 80,  weeklyGames: 2, monthlyGames: 3 },
  { username: "user10", tokens: 5,  weeklyStars: 25,  monthlyStars: 70,  weeklyGames: 1, monthlyGames: 3 },
  { username: "user11", tokens: 5,  weeklyStars: 15,  monthlyStars: 60,  weeklyGames: 1, monthlyGames: 2 },
  { username: "user12", tokens: 5,  weeklyStars: 5,   monthlyStars: 50,  weeklyGames: 1, monthlyGames: 2 },
];

export async function handleRestoreTestData(_req: Request): Promise<Response> {
  const stmt = db.prepare(
    "UPDATE users SET tokens=?, weekly_stars=?, monthly_stars=?, weekly_games=?, monthly_games=? WHERE username=?",
  );
  let updated = 0;
  for (const u of TEST_USER_STATS) {
    const result = stmt.run(u.tokens, u.weeklyStars, u.monthlyStars, u.weeklyGames, u.monthlyGames, u.username) as { changes: number };
    updated += result.changes;
  }
  return json({ ok: true, updated });
}

export async function handleResetDailyMissions(req: Request): Promise<Response> {
  let user_id: number | undefined;
  try {
    const body = await req.json() as { user_id?: number };
    user_id = body.user_id;
  } catch {}

  const today = new Date().toISOString().slice(0, 10);

  if (user_id) {
    db.query("DELETE FROM user_daily_missions WHERE user_id = ? AND date = ?").run(user_id, today);
    db.query("DELETE FROM user_daily_bonus WHERE user_id = ? AND date = ?").run(user_id, today);
  } else {
    db.query("DELETE FROM user_daily_missions WHERE date = ?").run(today);
    db.query("DELETE FROM user_daily_bonus WHERE date = ?").run(today);
  }

  return json({ ok: true, date: today });
}

const VALID_MISSION_KEYS = new Set<MissionKey>(["win_match", "send_chat", "friendly_match", "win_tournament"]);

export async function handleTriggerMission(req: Request): Promise<Response> {
  let body: unknown;
  try {
    body = await req.json();
  } catch {
    return json({ error: "Invalid JSON" }, 400);
  }

  const { user_id, mission_key } = body as { user_id?: number; mission_key?: string };
  if (!user_id) return json({ error: "user_id required" }, 400);
  if (!mission_key || !VALID_MISSION_KEYS.has(mission_key as MissionKey)) {
    return json({ error: "invalid mission_key" }, 400);
  }

  progressMission(user_id, mission_key as MissionKey);
  return json({ ok: true });
}

export async function handleSetUserStars(req: Request): Promise<Response> {
  let body: unknown;
  try {
    body = await req.json();
  } catch {
    return json({ error: "Invalid JSON" }, 400);
  }

  const { userId, weeklyStars, monthlyStars, weeklyGames, monthlyGames } = body as {
    userId: number;
    weeklyStars?: number;
    monthlyStars?: number;
    weeklyGames?: number;
    monthlyGames?: number;
  };

  if (!userId) return json({ error: "userId required" }, 400);

  db.query(`
    UPDATE users SET
      weekly_stars  = COALESCE(?, weekly_stars),
      monthly_stars = COALESCE(?, monthly_stars),
      weekly_games  = COALESCE(?, weekly_games),
      monthly_games = COALESCE(?, monthly_games)
    WHERE id = ?
  `).run(
    weeklyStars  !== undefined ? weeklyStars  : null,
    monthlyStars !== undefined ? monthlyStars : null,
    weeklyGames  !== undefined ? weeklyGames  : null,
    monthlyGames !== undefined ? monthlyGames : null,
    userId,
  );

  const user = db
    .query("SELECT id, username, weekly_stars, monthly_stars, weekly_games, monthly_games FROM users WHERE id = ?")
    .get(userId);
  return json({ ok: true, user });
}
