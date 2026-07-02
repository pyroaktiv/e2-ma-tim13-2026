import { db } from "./database";
import { generateRandomPoint } from "../util/regions";

type SeedUser = {
  email: string;
  username: string;
  password: string;
  region: string;
  tokens: number;
  weeklyStars: number;
  monthlyStars: number;
  weeklyGames: number;
  monthlyGames: number;
  totalStars: number;
  leagueId: number;
};

export async function seedUsers(): Promise<void> {
  const users: SeedUser[] = [
    // alice at rank 1 — use her account on the emulator to see the reward dialog
    { email: "alice@example.com",  username: "alice",  password: "Password1", region: "Beograd",                   tokens: 10, weeklyStars: 100, monthlyStars: 150, weeklyGames: 5, monthlyGames: 8, totalStars: 500, leagueId: 3 },
    // bob has 0 games — should NOT appear on the leaderboard (tests spec §4a)
    { email: "bob@example.com",    username: "bob",    password: "Password1", region: "Vojvodina",                  tokens: 5,  weeklyStars: 0,   monthlyStars: 0,   weeklyGames: 0, monthlyGames: 0, totalStars: 0,   leagueId: 1 },
    // 10 more users spread across regions and leagues to populate the leaderboard
    { email: "user3@example.com",  username: "user3",  password: "Password1", region: "Šumadija i Zapadna Srbija",  tokens: 5,  weeklyStars: 95,  monthlyStars: 140, weeklyGames: 4, monthlyGames: 7, totalStars: 420, leagueId: 3 },
    { email: "user4@example.com",  username: "user4",  password: "Password1", region: "Vojvodina",                  tokens: 5,  weeklyStars: 85,  monthlyStars: 130, weeklyGames: 4, monthlyGames: 6, totalStars: 360, leagueId: 3 },
    { email: "user5@example.com",  username: "user5",  password: "Password1", region: "Beograd",                    tokens: 5,  weeklyStars: 75,  monthlyStars: 120, weeklyGames: 3, monthlyGames: 5, totalStars: 280, leagueId: 2 },
    { email: "user6@example.com",  username: "user6",  password: "Password1", region: "Šumadija i Zapadna Srbija",  tokens: 5,  weeklyStars: 65,  monthlyStars: 110, weeklyGames: 3, monthlyGames: 5, totalStars: 220, leagueId: 2 },
    { email: "user7@example.com",  username: "user7",  password: "Password1", region: "Vojvodina",                  tokens: 5,  weeklyStars: 55,  monthlyStars: 100, weeklyGames: 2, monthlyGames: 4, totalStars: 160, leagueId: 2 },
    { email: "user8@example.com",  username: "user8",  password: "Password1", region: "Beograd",                    tokens: 5,  weeklyStars: 45,  monthlyStars: 90,  weeklyGames: 2, monthlyGames: 4, totalStars: 130, leagueId: 2 },
    { email: "user9@example.com",  username: "user9",  password: "Password1", region: "Južna i Istočna Srbija",     tokens: 5,  weeklyStars: 35,  monthlyStars: 80,  weeklyGames: 2, monthlyGames: 3, totalStars: 90,  leagueId: 1 },
    { email: "user10@example.com", username: "user10", password: "Password1", region: "Vojvodina",                  tokens: 5,  weeklyStars: 25,  monthlyStars: 70,  weeklyGames: 1, monthlyGames: 3, totalStars: 60,  leagueId: 1 },
    { email: "user11@example.com", username: "user11", password: "Password1", region: "Beograd",                    tokens: 5,  weeklyStars: 15,  monthlyStars: 60,  weeklyGames: 1, monthlyGames: 2, totalStars: 30,  leagueId: 1 },
    { email: "user12@example.com", username: "user12", password: "Password1", region: "Šumadija i Zapadna Srbija",  tokens: 5,  weeklyStars: 5,   monthlyStars: 50,  weeklyGames: 1, monthlyGames: 2, totalStars: 10,  leagueId: 1 },
  ];

  const today = new Date().toISOString().slice(0, 10);

  for (const u of users) {
    const existing = db
      .query("SELECT id FROM users WHERE email = ? OR username = ?")
      .get(u.email, u.username) as { id: number } | null;

    if (!existing) {
      const passwordHash = await Bun.password.hash(u.password);
      const qrToken = crypto.randomUUID();
      const { lat, lng } = generateRandomPoint(u.region);

      const { lastInsertRowid: userId } = db
        .query(
          `INSERT INTO users
             (email, username, password_hash, region, map_lat, map_lng, qr_token,
              tokens, email_verified, last_token_grant,
              weekly_stars, monthly_stars, weekly_games, monthly_games, total_stars, league_id)
           VALUES (?, ?, ?, ?, ?, ?, ?, ?, 1, ?, ?, ?, ?, ?, ?, ?)`,
        )
        .run(
          u.email, u.username, passwordHash, u.region, lat, lng, qrToken,
          u.tokens, today,
          u.weeklyStars, u.monthlyStars, u.weeklyGames, u.monthlyGames, u.totalStars, u.leagueId,
        );

      db.query("INSERT INTO match_summary (user_id) VALUES (?)").run(userId);
    } else if (u.weeklyGames > 0) {
      // Restore weekly/monthly stats that a cycle reset may have zeroed out.
      // Only restores a column when it was zeroed — leaves real game data intact.
      db.query(`
        UPDATE users SET
          weekly_stars  = CASE WHEN weekly_games  = 0 THEN ? ELSE weekly_stars  END,
          monthly_stars = CASE WHEN monthly_games = 0 THEN ? ELSE monthly_stars END,
          weekly_games  = CASE WHEN weekly_games  = 0 THEN ? ELSE weekly_games  END,
          monthly_games = CASE WHEN monthly_games = 0 THEN ? ELSE monthly_games END
        WHERE id = ?
      `).run(u.weeklyStars, u.monthlyStars, u.weeklyGames, u.monthlyGames, existing.id);
    }
  }
}
