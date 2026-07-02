import { db } from "../db/database";
import { createNotification } from "../routes/notifications";
import { pushToUser } from "./websocket";
import type { MissionKey, MissionStatus, DailyMissionsResponse } from "../model/mission";

const MISSION_DEFINITIONS: Array<{ key: MissionKey; title: string }> = [
  { key: "win_match",      title: "Pobedi partiju" },
  { key: "send_chat",      title: "Pošalji poruku u čet" },
  { key: "friendly_match", title: "Odigraj prijateljsku partiju" },
  { key: "win_tournament", title: "Pobedi partiju u turniru" },
];

const MISSION_STARS = 3;
const BONUS_TOKENS = 2;
const BONUS_STARS = 3;

function getTodayStr(): string {
  return new Date().toISOString().slice(0, 10);
}

function updateLeague(userId: number, newStars: number): void {
  const newLeague = db
    .query(
      `SELECT id, name FROM leagues WHERE min_stars <= ? ORDER BY min_stars DESC LIMIT 1`,
    )
    .get(newStars) as { id: number; name: string } | null;
  if (newLeague) {
    db.query("UPDATE users SET league_id = ? WHERE id = ?").run(newLeague.id, userId);
  }
}

function awardStars(userId: number, amount: number): number {
  const user = db
    .query("SELECT total_stars, weekly_stars, monthly_stars FROM users WHERE id = ?")
    .get(userId) as { total_stars: number; weekly_stars: number; monthly_stars: number } | null;
  if (!user) return 0;

  const newTotal = user.total_stars + amount;
  db.query(`
    UPDATE users
    SET total_stars   = ?,
        weekly_stars  = weekly_stars  + ?,
        monthly_stars = monthly_stars + ?,
        updated_at    = unixepoch()
    WHERE id = ?
  `).run(newTotal, amount, amount, userId);

  updateLeague(userId, newTotal);
  return newTotal;
}

function checkAndGrantBonus(userId: number, date: string): void {
  const row = db
    .query(
      "SELECT COUNT(*) AS cnt FROM user_daily_missions WHERE user_id = ? AND date = ? AND completed = 1",
    )
    .get(userId, date) as { cnt: number };

  if (row.cnt < MISSION_DEFINITIONS.length) return;

  const existing = db
    .query("SELECT 1 FROM user_daily_bonus WHERE user_id = ? AND date = ?")
    .get(userId, date);
  if (existing) return;

  db.query("INSERT INTO user_daily_bonus (user_id, date) VALUES (?, ?)").run(userId, date);

  awardStars(userId, BONUS_STARS);
  db.query("UPDATE users SET tokens = tokens + ? WHERE id = ?").run(BONUS_TOKENS, userId);

  createNotification(
    userId,
    "NAGRADE",
    "Sve dnevne misije završene!",
    `Završili ste sve dnevne misije i dobili ste ${BONUS_TOKENS} žetona i ${BONUS_STARS} zvezde bonus.`,
  );
  pushToUser(userId, { type: "mission_bonus", tokensEarned: BONUS_TOKENS, starsEarned: BONUS_STARS });
}

export function progressMission(userId: number, missionKey: MissionKey): void {
  const today = getTodayStr();

  const existing = db
    .query(
      "SELECT completed FROM user_daily_missions WHERE user_id = ? AND mission_key = ? AND date = ?",
    )
    .get(userId, missionKey, today) as { completed: number } | null;

  if (existing?.completed) return;

  db.query(`
    INSERT INTO user_daily_missions (user_id, mission_key, date, completed)
    VALUES (?, ?, ?, 1)
    ON CONFLICT(user_id, mission_key, date) DO UPDATE SET completed = 1
  `).run(userId, missionKey, today);

  awardStars(userId, MISSION_STARS);

  const def = MISSION_DEFINITIONS.find((m) => m.key === missionKey)!;
  createNotification(
    userId,
    "NAGRADE",
    "Misija završena!",
    `Završili ste misiju "${def.title}" i dobili ste ${MISSION_STARS} zvezde.`,
  );
  pushToUser(userId, { type: "mission_progress", missionKey, completed: true, starsEarned: MISSION_STARS });

  checkAndGrantBonus(userId, today);
}

export function getMissionsForUser(userId: number): DailyMissionsResponse {
  const today = getTodayStr();

  const rows = db
    .query(
      "SELECT mission_key FROM user_daily_missions WHERE user_id = ? AND date = ? AND completed = 1",
    )
    .all(userId, today) as Array<{ mission_key: string }>;

  const completedSet = new Set(rows.map((r) => r.mission_key));

  const bonusRow = db
    .query("SELECT 1 FROM user_daily_bonus WHERE user_id = ? AND date = ?")
    .get(userId, today);

  const missions: MissionStatus[] = MISSION_DEFINITIONS.map((def) => ({
    key: def.key,
    title: def.title,
    completed: completedSet.has(def.key),
    stars_reward: MISSION_STARS,
  }));

  return {
    date: today,
    missions,
    bonus: {
      all_complete: bonusRow !== null,
      tokens_reward: BONUS_TOKENS,
      stars_reward: BONUS_STARS,
    },
  };
}
