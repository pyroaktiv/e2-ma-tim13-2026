import { z } from "zod";
import { db } from "../../db/database";
import { requireAuth } from "../../middleware/auth";
import { json } from "../../util/response";

const IdSchema = z.coerce.number().int().positive();

export async function handleRemoveFriend(req: Request): Promise<Response> {
  const auth = await requireAuth(req);
  if (auth instanceof Response) return auth;

  const url = new URL(req.url);
  const rawId = url.pathname.split("/")[3];
  const parsed = IdSchema.safeParse(rawId);
  if (!parsed.success) return json({ error: "Invalid user ID" }, 400);

  const friendId = parsed.data;

  const result = db
    .query(
      `DELETE FROM friend_requests
       WHERE status = 'accepted'
         AND (
           (from_user_id = ? AND to_user_id = ?)
           OR (from_user_id = ? AND to_user_id = ?)
         )`,
    )
    .run(auth.user_id, friendId, friendId, auth.user_id);

  if (result.changes === 0) return json({ error: "Friend not found" }, 404);

  return json({ message: "Friend removed." });
}
