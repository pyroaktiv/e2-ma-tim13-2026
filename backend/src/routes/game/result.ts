import { z } from "zod";
import { db } from "../../db/database";
import { requireAuth } from "../../middleware/auth";
import { json } from "../../util/response";
import { createNotification } from "../notifications";

const ResultSchema = z.object({
  total_score: z.number().int().min(0),
  won: z.boolean(),
  is_friendly: z.boolean(),
});

export async function handleSubmitGameResult(req: Request): Promise<Response> {
  const auth = await requireAuth(req);
  if (auth instanceof Response) return auth;

  let body: unknown;
  try {
    body = await req.json();
  } catch {
    return json({ error: "Invalid JSON" }, 400);
  }

  const parsed = ResultSchema.safeParse(body);
  if (!parsed.success) return json({ error: parsed.error.issues[0]?.message }, 400);

  const { total_score, won, is_friendly } = parsed.data;

  if (is_friendly) {
    db.query(`
      INSERT INTO match_summary (user_id, total_games, wins, losses)
      VALUES (?, 1, 0, 0)
      ON CONFLICT(user_id) DO UPDATE SET
        total_games = total_games + 1,
        updated_at  = unixepoch()
    `).run(auth.user_id);
    return json({ star_delta: 0, tokens_earned: 0, message: "Friendly match recorded." });
  }

  const snapshot = db.transaction(() => {
    const current = db
      .query(
        "SELECT total_stars, weekly_stars, monthly_stars, tokens, league_id FROM users WHERE id = ?",
      )
      .get(auth.user_id) as {
        total_stars: number;
        weekly_stars: number;
        monthly_stars: number;
        tokens: number;
        league_id: number;
      } | null;

    if (!current) return null;

    const delta = won
      ? 10 + Math.floor(total_score / 40)
      : -10 + Math.floor(total_score / 40);

    const new_total   = Math.max(0, current.total_stars   + delta);
    const new_weekly  = Math.max(0, current.weekly_stars  + delta);
    const new_monthly = Math.max(0, current.monthly_stars + delta);

    db.query(`
      UPDATE users
      SET total_stars    = ?,
          weekly_stars   = ?,
          monthly_stars  = ?,
          weekly_games   = weekly_games  + 1,
          monthly_games  = monthly_games + 1,
          updated_at     = unixepoch()
      WHERE id = ?
    `).run(new_total, new_weekly, new_monthly, auth.user_id);

    const newLeague = db
      .query(
        `SELECT id, name, icon FROM leagues
         WHERE min_stars <= ?
         ORDER BY min_stars DESC
         LIMIT 1`,
      )
      .get(new_total) as { id: number; name: string; icon: string } | null;

    let leagueChanged = false;
    if (newLeague && newLeague.id !== current.league_id) {
      db.query("UPDATE users SET league_id = ? WHERE id = ?").run(newLeague.id, auth.user_id);
      leagueChanged = true;
      const direction = newLeague.id > current.league_id ? "prešli ste u višu ligu" : "pali ste na nižu ligu";
      createNotification(
        auth.user_id,
        "OSTALO",
        `Promena lige: ${newLeague.name}`,
        `Nakon poslednje partije, ${direction}: ${newLeague.name}.`,
      );
    }

    db.query(`
      INSERT INTO match_summary (user_id, total_games, wins, losses)
      VALUES (?, 1, ?, ?)
      ON CONFLICT(user_id) DO UPDATE SET
        total_games = total_games + 1,
        wins        = wins   + excluded.wins,
        losses      = losses + excluded.losses,
        updated_at  = unixepoch()
    `).run(auth.user_id, won ? 1 : 0, won ? 0 : 1);

    const tokens_to_add =
      Math.floor(new_total / 50) - Math.floor(current.total_stars / 50);

    if (tokens_to_add > 0) {
      db.query("UPDATE users SET tokens = tokens + ? WHERE id = ?").run(tokens_to_add, auth.user_id);
      createNotification(
        auth.user_id,
        "NAGRADE",
        "Token nagrada!",
        `Dostigli ste novi prag od ${Math.floor(new_total / 50) * 50} zvezda i dobili ste ${tokens_to_add} žeton${tokens_to_add === 1 ? "" : "a"}.`,
      );
    }

    const finalUser = db
      .query(
        `SELECT u.total_stars, u.weekly_stars, u.monthly_stars, u.tokens,
                l.id AS league_id, l.name AS league_name, l.icon AS league_icon
         FROM users u JOIN leagues l ON l.id = u.league_id
         WHERE u.id = ?`,
      )
      .get(auth.user_id) as {
        total_stars: number;
        weekly_stars: number;
        monthly_stars: number;
        tokens: number;
        league_id: number;
        league_name: string;
        league_icon: string;
      } | null;

    return { delta, tokens_to_add, leagueChanged, finalUser };
  })();

  if (!snapshot || !snapshot.finalUser) {
    return json({ error: "User not found" }, 404);
  }

  return json({
    star_delta:    snapshot.delta,
    total_stars:   snapshot.finalUser.total_stars,
    weekly_stars:  snapshot.finalUser.weekly_stars,
    monthly_stars: snapshot.finalUser.monthly_stars,
    tokens:        snapshot.finalUser.tokens,
    tokens_earned: snapshot.tokens_to_add,
    league: {
      id:   snapshot.finalUser.league_id,
      name: snapshot.finalUser.league_name,
      icon: snapshot.finalUser.league_icon,
    },
  });
}
