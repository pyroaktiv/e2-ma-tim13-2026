import { db } from "../../db/database";
import { json } from "../../util/response";
import { checkAndRunWeeklyReset } from "../../util/weekly";
import { checkAndRunMonthlyReset } from "../../util/monthly";

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
  { username: "alice",  weeklyStars: 100, monthlyStars: 150, weeklyGames: 5, monthlyGames: 8 },
  { username: "user3",  weeklyStars: 95,  monthlyStars: 140, weeklyGames: 4, monthlyGames: 7 },
  { username: "user4",  weeklyStars: 85,  monthlyStars: 130, weeklyGames: 4, monthlyGames: 6 },
  { username: "user5",  weeklyStars: 75,  monthlyStars: 120, weeklyGames: 3, monthlyGames: 5 },
  { username: "user6",  weeklyStars: 65,  monthlyStars: 110, weeklyGames: 3, monthlyGames: 5 },
  { username: "user7",  weeklyStars: 55,  monthlyStars: 100, weeklyGames: 2, monthlyGames: 4 },
  { username: "user8",  weeklyStars: 45,  monthlyStars: 90,  weeklyGames: 2, monthlyGames: 4 },
  { username: "user9",  weeklyStars: 35,  monthlyStars: 80,  weeklyGames: 2, monthlyGames: 3 },
  { username: "user10", weeklyStars: 25,  monthlyStars: 70,  weeklyGames: 1, monthlyGames: 3 },
  { username: "user11", weeklyStars: 15,  monthlyStars: 60,  weeklyGames: 1, monthlyGames: 2 },
  { username: "user12", weeklyStars: 5,   monthlyStars: 50,  weeklyGames: 1, monthlyGames: 2 },
];

export async function handleRestoreTestData(_req: Request): Promise<Response> {
  const stmt = db.prepare(
    "UPDATE users SET weekly_stars=?, monthly_stars=?, weekly_games=?, monthly_games=? WHERE username=?",
  );
  let updated = 0;
  for (const u of TEST_USER_STATS) {
    const result = stmt.run(u.weeklyStars, u.monthlyStars, u.weeklyGames, u.monthlyGames, u.username) as { changes: number };
    updated += result.changes;
  }
  return json({ ok: true, updated });
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
