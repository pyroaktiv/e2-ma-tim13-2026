import { z } from "zod";
import { db } from "../../db/database";
import { requireAuth } from "../../middleware/auth";
import { json } from "../../util/response";
import { pushToUser } from "../../util/websocket";
import { createNotification } from "../notifications";
import type { FriendRequestRow } from "../../model/friend";
import type { UserRow } from "../../model/user";

const SendRequestSchema = z
  .object({
    username: z.string().optional(),
    qr_token: z.string().optional(),
  })
  .refine((d) => d.username || d.qr_token, {
    message: "username or qr_token is required",
  });

const IdSchema = z.coerce.number().int().positive();

export async function handleSendFriendRequest(req: Request): Promise<Response> {
  const auth = await requireAuth(req);
  if (auth instanceof Response) return auth;

  let body: unknown;
  try {
    body = await req.json();
  } catch {
    return json({ error: "Invalid JSON" }, 400);
  }

  const result = SendRequestSchema.safeParse(body);
  if (!result.success)
    return json({ error: result.error.issues[0]?.message }, 400);

  const { username, qr_token } = result.data;

  const target = db
    .query(
      username
        ? "SELECT id, username FROM users WHERE username = ? COLLATE NOCASE"
        : "SELECT id, username FROM users WHERE qr_token = ?",
    )
    .get((username ?? qr_token) as string) as Pick<
    UserRow,
    "id" | "username"
  > | null;

  if (!target) return json({ error: "User not found" }, 404);
  if (target.id === auth.user_id)
    return json({ error: "Cannot add yourself" }, 400);

  const existing = db
    .query(
      `SELECT id FROM friend_requests
       WHERE (from_user_id = ? AND to_user_id = ?) OR (from_user_id = ? AND to_user_id = ?)`,
    )
    .get(auth.user_id, target.id, target.id, auth.user_id) as {
    id: number;
  } | null;

  if (existing) return json({ error: "Friend request already exists" }, 409);

  db.query(
    "INSERT INTO friend_requests (from_user_id, to_user_id) VALUES (?, ?)",
  ).run(auth.user_id, target.id);

  createNotification(
    target.id,
    "OSTALO",
    "Friend Request",
    `${auth.username} sent you a friend request.`,
  );
  pushToUser(target.id, {
    type: "friend_request",
    from: { id: auth.user_id, username: auth.username },
  });

  return json({ message: "Friend request sent." }, 201);
}

export async function handleGetFriendRequests(req: Request): Promise<Response> {
  const auth = await requireAuth(req);
  if (auth instanceof Response) return auth;

  const rows = db
    .query(
      `SELECT fr.id, fr.from_user_id, fr.created_at,
              u.username AS from_username, u.avatar AS from_avatar
       FROM friend_requests fr
       JOIN users u ON u.id = fr.from_user_id
       WHERE fr.to_user_id = ? AND fr.status = 'pending'
       ORDER BY fr.created_at DESC`,
    )
    .all(auth.user_id) as Array<{
    id: number;
    from_user_id: number;
    created_at: number;
    from_username: string;
    from_avatar: string;
  }>;

  return json(
    rows.map((r) => ({
      id: r.id,
      from: {
        id: r.from_user_id,
        username: r.from_username,
        avatar: r.from_avatar,
      },
      created_at: new Date(r.created_at * 1000).toISOString(),
    })),
  );
}

export async function handleAcceptFriendRequest(
  req: Request,
): Promise<Response> {
  const auth = await requireAuth(req);
  if (auth instanceof Response) return auth;

  const url = new URL(req.url);
  const rawId = url.pathname.split("/")[4];
  const parsed = IdSchema.safeParse(rawId);
  if (!parsed.success) return json({ error: "Invalid request ID" }, 400);

  const fr = db
    .query(
      "SELECT * FROM friend_requests WHERE id = ? AND to_user_id = ? AND status = 'pending'",
    )
    .get(parsed.data, auth.user_id) as FriendRequestRow | null;

  if (!fr) return json({ error: "Friend request not found" }, 404);

  db.query("UPDATE friend_requests SET status = 'accepted' WHERE id = ?").run(
    fr.id,
  );

  createNotification(
    fr.from_user_id,
    "OSTALO",
    "Friend Request Accepted",
    `${auth.username} accepted your friend request.`,
  );
  pushToUser(fr.from_user_id, {
    type: "friend_request_accepted",
    by: { id: auth.user_id, username: auth.username },
  });

  return json({ message: "Friend request accepted." });
}

export async function handleDeclineFriendRequest(
  req: Request,
): Promise<Response> {
  const auth = await requireAuth(req);
  if (auth instanceof Response) return auth;

  const url = new URL(req.url);
  const rawId = url.pathname.split("/")[4];
  const parsed = IdSchema.safeParse(rawId);
  if (!parsed.success) return json({ error: "Invalid request ID" }, 400);

  const result = db
    .query(
      "UPDATE friend_requests SET status = 'declined' WHERE id = ? AND to_user_id = ? AND status = 'pending'",
    )
    .run(parsed.data, auth.user_id);

  if (result.changes === 0)
    return json({ error: "Friend request not found" }, 404);

  return json({ message: "Friend request declined." });
}
