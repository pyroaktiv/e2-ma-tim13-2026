import { db } from "../../db/database";
import { requireAuth } from "../../middleware/auth";
import { json } from "../../util/response";

type RelationshipStatus = "none" | "pending_sent" | "pending_received" | "friends";

interface SearchResult {
  id: number;
  username: string;
  avatar: string;
  relationship: RelationshipStatus;
}

export async function handleSearchUsers(req: Request): Promise<Response> {
  const auth = await requireAuth(req);
  if (auth instanceof Response) return auth;

  const url = new URL(req.url);
  const q = url.searchParams.get("q")?.trim();
  if (!q) return json({ error: "Query parameter 'q' is required" }, 400);

  const rows = db
    .query(
      `SELECT u.id, u.username, u.avatar,
              fr.id AS fr_id, fr.from_user_id, fr.to_user_id, fr.status
       FROM users u
       LEFT JOIN friend_requests fr ON (
         (fr.from_user_id = u.id AND fr.to_user_id = ?)
         OR (fr.to_user_id = u.id AND fr.from_user_id = ?)
       )
       WHERE u.id != ?
         AND (u.username LIKE ? OR u.qr_token = ?)
       ORDER BY u.username ASC
       LIMIT 20`,
    )
    .all(auth.user_id, auth.user_id, auth.user_id, `%${q}%`, q) as Array<{
    id: number;
    username: string;
    avatar: string;
    fr_id: number | null;
    from_user_id: number | null;
    to_user_id: number | null;
    status: string | null;
  }>;

  const results: SearchResult[] = rows.map((u) => {
    let relationship: RelationshipStatus = "none";
    if (u.fr_id !== null) {
      if (u.status === "accepted") {
        relationship = "friends";
      } else if (u.status === "pending" && u.from_user_id === auth.user_id) {
        relationship = "pending_sent";
      } else if (u.status === "pending" && u.to_user_id === auth.user_id) {
        relationship = "pending_received";
      }
    }
    return { id: u.id, username: u.username, avatar: u.avatar, relationship };
  });

  return json(results);
}
