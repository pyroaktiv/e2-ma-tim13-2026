import { db } from "../../db/database";
import { requireAuth } from "../../middleware/auth";
import { json } from "../../util/response";
import type { UserProfile } from "../../model/user";

export async function handleGetProfile(req: Request): Promise<Response> {
  const auth = await requireAuth(req);
  if (auth instanceof Response) return auth;

  const row = db
    .query(
      `SELECT u.id, u.username, u.email, u.avatar, u.tokens, u.total_stars,
              u.region, u.qr_token, l.name AS league_name, l.icon AS league_icon
       FROM users u
       JOIN leagues l ON l.id = u.league_id
       WHERE u.id = ?`
    )
    .get(auth.user_id) as
    | (Omit<UserProfile, "league"> & { league_name: string; league_icon: string })
    | null;

  if (!row) return json({ error: "User not found" }, 404);

  const profile: UserProfile = {
    id: row.id,
    username: row.username,
    email: row.email,
    avatar: row.avatar,
    tokens: row.tokens,
    total_stars: row.total_stars,
    league: { name: row.league_name, icon: row.league_icon },
    region: row.region,
    qr_token: row.qr_token,
  };

  return json(profile);
}
