import { db } from "../db/database";
import { requireAuth } from "../middleware/auth";
import { json } from "../util/response";
import { isOnline } from "../util/websocket";
import type { ChatConversationRow, ChatMessageRow } from "../model/chat";

const MAX_PATTERN_LENGTH = 100;

/** Lista konverzacija (spec 8) — vidljiva čim postoji bar jedna poruka u bilo kom smeru. */
export async function handleGetConversations(req: Request): Promise<Response> {
  const auth = await requireAuth(req);
  if (auth instanceof Response) return auth;
  const me = auth.user_id;

  const rows = db
    .query(
      `WITH convo AS (
         SELECT CASE WHEN from_user_id = ? THEN to_user_id ELSE from_user_id END AS other_id,
                MAX(created_at) AS last_at
         FROM chat_messages
         WHERE from_user_id = ? OR to_user_id = ?
         GROUP BY other_id
       )
       SELECT c.other_id, c.last_at, u.username, u.avatar,
         (SELECT body FROM chat_messages m
            WHERE (m.from_user_id = c.other_id AND m.to_user_id = ?)
               OR (m.from_user_id = ? AND m.to_user_id = c.other_id)
            ORDER BY m.created_at DESC LIMIT 1) AS last_body,
         (SELECT m.from_user_id FROM chat_messages m
            WHERE (m.from_user_id = c.other_id AND m.to_user_id = ?)
               OR (m.from_user_id = ? AND m.to_user_id = c.other_id)
            ORDER BY m.created_at DESC LIMIT 1) AS last_from_user_id,
         (SELECT COUNT(*) FROM chat_messages m
            WHERE m.from_user_id = c.other_id AND m.to_user_id = ? AND m.is_read = 0) AS unread_count
       FROM convo c
       JOIN users u ON u.id = c.other_id
       ORDER BY c.last_at DESC`,
    )
    .all(me, me, me, me, me, me, me, me) as Array<
    ChatConversationRow & { last_body: string | null; last_from_user_id: number | null }
  >;

  return json(
    rows.map((r) => ({
      user_id: r.other_id,
      username: r.username,
      avatar: r.avatar,
      last_message: r.last_body,
      last_message_mine: r.last_from_user_id === me,
      last_message_at: new Date(r.last_at * 1000).toISOString(),
      unread_count: r.unread_count,
      is_online: isOnline(r.other_id),
    })),
  );
}

/** Istorija poruke sa jednim korisnikom; čitanjem se nepročitane poruke od njega označe pročitanim. */
export async function handleGetMessages(req: Request): Promise<Response> {
  const auth = await requireAuth(req);
  if (auth instanceof Response) return auth;
  const me = auth.user_id;

  const otherId = Number(new URL(req.url).pathname.split("/")[4]);
  if (!Number.isInteger(otherId) || otherId <= 0) {
    return json({ error: "Invalid user ID" }, 400);
  }

  const other = db.query("SELECT id, username, avatar, region FROM users WHERE id = ?").get(otherId) as
    | { id: number; username: string; avatar: string; region: string }
    | null;
  if (!other) return json({ error: "User not found" }, 404);

  db.query(
    "UPDATE chat_messages SET is_read = 1 WHERE from_user_id = ? AND to_user_id = ? AND is_read = 0",
  ).run(otherId, me);

  const rows = db
    .query(
      `SELECT id, from_user_id, to_user_id, body, is_read, created_at FROM chat_messages
       WHERE (from_user_id = ? AND to_user_id = ?) OR (from_user_id = ? AND to_user_id = ?)
       ORDER BY created_at ASC`,
    )
    .all(me, otherId, otherId, me) as ChatMessageRow[];

  return json({
    user: { id: other.id, username: other.username, avatar: other.avatar },
    messages: rows.map((m) => ({
      id: m.id,
      from_user_id: m.from_user_id,
      to_user_id: m.to_user_id,
      body: m.body,
      created_at: new Date(m.created_at * 1000).toISOString(),
    })),
  });
}

/** Pretraga korisnika iz istog regiona po regex-u nad username-om (spec 8). */
export async function handleSearchChatUsers(req: Request): Promise<Response> {
  const auth = await requireAuth(req);
  if (auth instanceof Response) return auth;

  const pattern = new URL(req.url).searchParams.get("q")?.trim();
  if (!pattern) return json({ error: "Query parameter 'q' is required" }, 400);
  if (pattern.length > MAX_PATTERN_LENGTH) {
    return json({ error: "Pattern too long" }, 400);
  }

  let regex: RegExp;
  try {
    regex = new RegExp(pattern, "i");
  } catch {
    return json({ error: "Invalid regex pattern" }, 400);
  }

  const me = db.query("SELECT region FROM users WHERE id = ?").get(auth.user_id) as { region: string } | null;
  if (!me) return json({ error: "User not found" }, 404);

  const candidates = db
    .query("SELECT id, username, avatar FROM users WHERE region = ? AND id != ? ORDER BY username ASC")
    .all(me.region, auth.user_id) as Array<{ id: number; username: string; avatar: string }>;

  const matches = candidates.filter((u) => regex.test(u.username)).slice(0, 50);

  return json(matches.map((u) => ({ id: u.id, username: u.username, avatar: u.avatar })));
}
