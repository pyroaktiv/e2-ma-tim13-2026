import { db } from "../db/database";
import { createNotification } from "../routes/notifications";
import { pushToUser } from "./websocket";

const WEEKLY_REWARDS: Record<number, number> = {
  1: 5, 2: 3, 3: 2,
  4: 1, 5: 1, 6: 1, 7: 1, 8: 1, 9: 1, 10: 1,
};

function getISOWeek(date: Date): { year: number; week: number } {
  const d = new Date(Date.UTC(date.getFullYear(), date.getMonth(), date.getDate()));
  const dayNum = d.getUTCDay() || 7;
  d.setUTCDate(d.getUTCDate() + 4 - dayNum);
  const yearStart = new Date(Date.UTC(d.getUTCFullYear(), 0, 1));
  const week = Math.ceil((((d.getTime() - yearStart.getTime()) / 86400000) + 1) / 7);
  return { year: d.getUTCFullYear(), week };
}

export function getCurrentWeekKey(): string {
  const { year, week } = getISOWeek(new Date());
  return `${year}-${String(week).padStart(2, "0")}`;
}

export function getWeekBounds(weekKey: string): { start: Date; end: Date } {
  const [yearStr, weekStr] = weekKey.split("-");
  const year = parseInt(yearStr!);
  const week = parseInt(weekStr!);
  const jan4 = new Date(Date.UTC(year, 0, 4));
  const dayOfWeek = jan4.getUTCDay() || 7;
  const week1Mon = new Date(jan4);
  week1Mon.setUTCDate(jan4.getUTCDate() - (dayOfWeek - 1));
  const start = new Date(week1Mon);
  start.setUTCDate(week1Mon.getUTCDate() + (week - 1) * 7);
  const end = new Date(start);
  end.setUTCDate(start.getUTCDate() + 6);
  end.setUTCHours(23, 59, 59, 999);
  return { start, end };
}

export function getCurrentWeekBounds(): { start: Date; end: Date } {
  return getWeekBounds(getCurrentWeekKey());
}

export function checkAndRunWeeklyReset(): void {
  const currentWeekKey = getCurrentWeekKey();

  const config = db
    .query("SELECT value FROM server_config WHERE key = 'last_weekly_reset'")
    .get() as { value: string } | null;

  if (!config || config.value === currentWeekKey) return;

  const top10 = db
    .query(
      `SELECT id, username, weekly_stars
       FROM users
       WHERE weekly_games > 0
       ORDER BY weekly_stars DESC
       LIMIT 10`,
    )
    .all() as Array<{ id: number; username: string; weekly_stars: number }>;

  const { year, week } = parseWeekKey(config.value);

  const reset = db.transaction(() => {
    for (let i = 0; i < top10.length; i++) {
      const rank = i + 1;
      const tokens = WEEKLY_REWARDS[rank] ?? 0;
      if (tokens === 0) continue;
      const user = top10[i]!;
      db.query("UPDATE users SET tokens = tokens + ? WHERE id = ?").run(tokens, user.id);
      createNotification(
        user.id,
        "NAGRADE",
        "Nedeljni turnir – nagrada!",
        `Završili ste na ${rank}. mestu u nedeljnoj rang listi i dobili ste ${tokens} žeton${tokens === 1 ? "" : "a"}.`,
      );
    }
    db.query("UPDATE users SET weekly_stars = 0, weekly_games = 0").run();
    db.query("UPDATE server_config SET value = ? WHERE key = 'last_weekly_reset'").run(currentWeekKey);
  });

  reset();

  for (let i = 0; i < top10.length; i++) {
    const rank = i + 1;
    const tokens = WEEKLY_REWARDS[rank] ?? 0;
    if (tokens === 0) continue;
    pushToUser(top10[i]!.id, { type: "cycle_reward", cycle: "weekly", rank, tokens });
  }

  console.log(`Weekly reset completed for ${year}-${String(week).padStart(2, "0")}`);
}

function parseWeekKey(weekKey: string): { year: number; week: number } {
  const [y, w] = weekKey.split("-");
  return { year: parseInt(y!), week: parseInt(w!) };
}
