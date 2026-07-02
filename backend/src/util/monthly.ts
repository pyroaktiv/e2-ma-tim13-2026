import { db } from "../db/database";
import { createNotification } from "../routes/notifications";
import { pushToUser } from "./websocket";

function getCurrentYYYYMM(): string {
  const now = new Date();
  return `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, "0")}`;
}

function parseYYYYMM(yyyymm: string): { year: number; month: number } {
  const parts = yyyymm.split("-");
  return { year: parseInt(parts[0]!), month: parseInt(parts[1]!) };
}

const MONTHLY_REWARDS: Record<number, number> = {
  1: 10, 2: 6, 3: 4,
  4: 2, 5: 2, 6: 2, 7: 2, 8: 2, 9: 2, 10: 2,
};

export function checkAndRunMonthlyReset(): void {
  const currentYYYYMM = getCurrentYYYYMM();

  const config = db
    .query("SELECT value FROM server_config WHERE key = 'last_monthly_reset'")
    .get() as { value: string } | null;

  if (!config || config.value === currentYYYYMM) return;

  const { year, month } = parseYYYYMM(config.value);

  const regionStats = db
    .query(
      `SELECT region, COALESCE(SUM(monthly_stars), 0) AS total_stars
       FROM users
       WHERE region IS NOT NULL
       GROUP BY region
       ORDER BY total_stars DESC`,
    )
    .all() as Array<{ region: string; total_stars: number }>;

  const top10 = db
    .query(
      `SELECT id, username, monthly_stars
       FROM users
       WHERE monthly_games > 0
       ORDER BY monthly_stars DESC
       LIMIT 10`,
    )
    .all() as Array<{ id: number; username: string; monthly_stars: number }>;

  const frameByRank: Record<number, string> = { 1: "gold", 2: "silver", 3: "bronze" };

  const reset = db.transaction(() => {
    const insertStat = db.prepare(
      "INSERT OR REPLACE INTO monthly_region_stats (region_name, year, month, total_stars, rank) VALUES (?, ?, ?, ?, ?)",
    );
    for (let i = 0; i < regionStats.length; i++) {
      const stat = regionStats[i]!;
      insertStat.run(stat.region, year, month, stat.total_stars, i + 1);
    }

    db.query("UPDATE users SET avatar_frame = 'none'").run();
    for (let i = 0; i < Math.min(3, regionStats.length); i++) {
      const frame = frameByRank[i + 1]!;
      const stat = regionStats[i]!;
      db.query("UPDATE users SET avatar_frame = ? WHERE region = ?").run(frame, stat.region);
    }

    for (let i = 0; i < top10.length; i++) {
      const rank = i + 1;
      const tokens = MONTHLY_REWARDS[rank] ?? 0;
      if (tokens === 0) continue;
      const user = top10[i]!;
      db.query("UPDATE users SET tokens = tokens + ? WHERE id = ?").run(tokens, user.id);
      createNotification(
        user.id,
        "NAGRADE",
        "Mesečni turnir – nagrada!",
        `Završili ste na ${rank}. mestu u mesečnoj rang listi i dobili ste ${tokens} žeton${tokens === 1 ? "" : "a"}.`,
      );
    }

    // Players who didn't participate lose 30% of total_stars (spec §6e)
    db.query(`
      UPDATE users
      SET total_stars = CAST(total_stars * 0.7 AS INTEGER)
      WHERE monthly_games = 0 AND total_stars > 0
    `).run();
    // Re-derive league for those affected
    db.query(`
      UPDATE users
      SET league_id = (
        SELECT id FROM leagues
        WHERE min_stars <= users.total_stars
        ORDER BY min_stars DESC
        LIMIT 1
      )
      WHERE monthly_games = 0
    `).run();

    db.query("UPDATE users SET monthly_stars = 0").run();
    db.query("UPDATE users SET monthly_games = 0").run();

    db.query("UPDATE server_config SET value = ? WHERE key = 'last_monthly_reset'").run(currentYYYYMM);
  });

  reset();

  for (let i = 0; i < top10.length; i++) {
    const rank = i + 1;
    const tokens = MONTHLY_REWARDS[rank] ?? 0;
    if (tokens === 0) continue;
    pushToUser(top10[i]!.id, { type: "cycle_reward", cycle: "monthly", rank, tokens });
  }

  console.log(
    `Monthly reset completed for ${year}-${String(month).padStart(2, "0")}`,
  );
}
