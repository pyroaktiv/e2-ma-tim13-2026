import { db } from "../../db/database";
import { requireAuth } from "../../middleware/auth";
import { json } from "../../util/response";
import { isOnline } from "../../util/websocket";
import { getRank } from "../../ranking/cycles";
import type { FriendProfile } from "../../model/friend";

export async function handleGetFriends(req: Request): Promise<Response> {
  const auth = await requireAuth(req);
  if (auth instanceof Response) return auth;

  const rows = db
    .query(
      `SELECT
         fr.id AS friendship_id,
         CASE WHEN fr.from_user_id = ? THEN fr.to_user_id ELSE fr.from_user_id END AS friend_id,
         u.username, u.avatar, u.total_stars, u.in_game,
         l.name AS league_name, l.icon AS league_icon
       FROM friend_requests fr
       JOIN users u ON u.id = CASE WHEN fr.from_user_id = ? THEN fr.to_user_id ELSE fr.from_user_id END
       JOIN leagues l ON l.id = u.league_id
       WHERE (fr.from_user_id = ? OR fr.to_user_id = ?) AND fr.status = 'accepted'
       ORDER BY u.username ASC`,
    )
    .all(
      auth.user_id,
      auth.user_id,
      auth.user_id,
      auth.user_id,
    ) as Array<{
      friendship_id: number;
      friend_id: number;
      username: string;
      avatar: string;
      total_stars: number;
      in_game: number;
      league_name: string;
      league_icon: string;
    }>;

  const friends: FriendProfile[] = rows.map((row) => ({
    id: row.friend_id,
    username: row.username,
    avatar: row.avatar,
    total_stars: row.total_stars,
    league: { name: row.league_name, icon: row.league_icon },
    monthly_rank: getRank(row.friend_id, "monthly"),
    is_online: isOnline(row.friend_id),
    in_game: row.in_game === 1,
    friendship_id: row.friendship_id,
  }));

  return json(friends);
}
