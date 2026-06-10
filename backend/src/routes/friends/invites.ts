import { z } from "zod";
import { db } from "../../db/database";
import { requireAuth } from "../../middleware/auth";
import { json } from "../../util/response";
import { isOnline, pushToUser } from "../../util/websocket";
import { createNotification } from "../notifications";
import type { GameInviteRow } from "../../model/friend";

const SendInviteSchema = z.object({
  to_user_id: z.number().int().positive(),
});

const IdSchema = z.coerce.number().int().positive();

function expireStaleInvites(): void {
  db.query(
    "UPDATE game_invites SET status = 'expired' WHERE status = 'pending' AND expires_at < unixepoch()",
  ).run();
}

export async function handleSendGameInvite(req: Request): Promise<Response> {
  const auth = await requireAuth(req);
  if (auth instanceof Response) return auth;

  let body: unknown;
  try {
    body = await req.json();
  } catch {
    return json({ error: "Invalid JSON" }, 400);
  }

  const result = SendInviteSchema.safeParse(body);
  if (!result.success)
    return json({ error: result.error.issues[0]?.message }, 400);

  const { to_user_id } = result.data;

  if (to_user_id === auth.user_id)
    return json({ error: "Cannot invite yourself" }, 400);

  const friendship = db
    .query(
      `SELECT id FROM friend_requests
       WHERE ((from_user_id = ? AND to_user_id = ?) OR (from_user_id = ? AND to_user_id = ?))
         AND status = 'accepted'`,
    )
    .get(auth.user_id, to_user_id, to_user_id, auth.user_id) as {
    id: number;
  } | null;

  if (!friendship) return json({ error: "Not friends with this user" }, 400);

  if (!isOnline(to_user_id)) return json({ error: "User is not online" }, 400);

  const target = db
    .query("SELECT in_game FROM users WHERE id = ?")
    .get(to_user_id) as { in_game: number } | null;

  if (!target) return json({ error: "User not found" }, 404);
  if (target.in_game)
    return json({ error: "User is currently in a game" }, 400);

  const existing = db
    .query(
      `SELECT id FROM game_invites
       WHERE ((from_user_id = ? AND to_user_id = ?) OR (from_user_id = ? AND to_user_id = ?))
         AND status = 'pending'`,
    )
    .get(auth.user_id, to_user_id, to_user_id, auth.user_id) as {
    id: number;
  } | null;

  if (existing) return json({ error: "A game invite already exists" }, 409);

  const expiresAt = Math.floor(Date.now() / 1000) + 10;
  const insertResult = db
    .query(
      "INSERT INTO game_invites (from_user_id, to_user_id, expires_at) VALUES (?, ?, ?)",
    )
    .run(auth.user_id, to_user_id, expiresAt);

  const inviteId = Number(insertResult.lastInsertRowid);
  const fromUserId = auth.user_id;

  pushToUser(to_user_id, {
    type: "game_invite",
    invite: {
      id: inviteId,
      from: { id: fromUserId, username: auth.username },
      expires_at: new Date(expiresAt * 1000).toISOString(),
    },
  });
  createNotification(
    to_user_id,
    "OSTALO",
    "Game Invite",
    `${auth.username} invited you to a game.`,
  );

  setTimeout(() => {
    const changed = db
      .query(
        "UPDATE game_invites SET status = 'expired' WHERE id = ? AND status = 'pending'",
      )
      .run(inviteId);
    if (changed.changes > 0) {
      pushToUser(fromUserId, { type: "invite_expired", invite_id: inviteId });
      pushToUser(to_user_id, { type: "invite_expired", invite_id: inviteId });
    }
  }, 10_000);

  return json(
    { id: inviteId, expires_at: new Date(expiresAt * 1000).toISOString() },
    201,
  );
}

export async function handleGetGameInvites(req: Request): Promise<Response> {
  const auth = await requireAuth(req);
  if (auth instanceof Response) return auth;

  expireStaleInvites();

  const rows = db
    .query(
      `SELECT gi.id, gi.from_user_id, gi.created_at, gi.expires_at,
              u.username AS from_username, u.avatar AS from_avatar
       FROM game_invites gi
       JOIN users u ON u.id = gi.from_user_id
       WHERE gi.to_user_id = ? AND gi.status = 'pending'
       ORDER BY gi.created_at DESC`,
    )
    .all(auth.user_id) as Array<{
    id: number;
    from_user_id: number;
    created_at: number;
    expires_at: number;
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
      expires_at: new Date(r.expires_at * 1000).toISOString(),
    })),
  );
}

export async function handleGetSentGameInvites(
  req: Request,
): Promise<Response> {
  const auth = await requireAuth(req);
  if (auth instanceof Response) return auth;

  expireStaleInvites();

  const rows = db
    .query(
      `SELECT gi.id, gi.to_user_id, gi.created_at, gi.expires_at,
              u.username AS to_username, u.avatar AS to_avatar
       FROM game_invites gi
       JOIN users u ON u.id = gi.to_user_id
       WHERE gi.from_user_id = ? AND gi.status = 'pending'
       ORDER BY gi.created_at DESC`,
    )
    .all(auth.user_id) as Array<{
    id: number;
    to_user_id: number;
    created_at: number;
    expires_at: number;
    to_username: string;
    to_avatar: string;
  }>;

  return json(
    rows.map((r) => ({
      id: r.id,
      to: { id: r.to_user_id, username: r.to_username, avatar: r.to_avatar },
      created_at: new Date(r.created_at * 1000).toISOString(),
      expires_at: new Date(r.expires_at * 1000).toISOString(),
    })),
  );
}

export async function handleAcceptGameInvite(req: Request): Promise<Response> {
  const auth = await requireAuth(req);
  if (auth instanceof Response) return auth;

  const url = new URL(req.url);
  const rawId = url.pathname.split("/")[4];
  const parsed = IdSchema.safeParse(rawId);
  if (!parsed.success) return json({ error: "Invalid invite ID" }, 400);

  const invite = db
    .query(
      "SELECT * FROM game_invites WHERE id = ? AND to_user_id = ? AND status = 'pending'",
    )
    .get(parsed.data, auth.user_id) as GameInviteRow | null;

  if (!invite)
    return json({ error: "Invite not found or already resolved" }, 404);

  if (invite.expires_at < Math.floor(Date.now() / 1000)) {
    db.query("UPDATE game_invites SET status = 'expired' WHERE id = ?").run(
      invite.id,
    );
    return json({ error: "Invite has expired" }, 410);
  }

  db.query("UPDATE game_invites SET status = 'accepted' WHERE id = ?").run(
    invite.id,
  );

  pushToUser(invite.from_user_id, {
    type: "invite_accepted",
    invite_id: invite.id,
    by: { id: auth.user_id, username: auth.username },
  });

  return json({ message: "Game invite accepted." });
}

export async function handleDeclineGameInvite(req: Request): Promise<Response> {
  const auth = await requireAuth(req);
  if (auth instanceof Response) return auth;

  const url = new URL(req.url);
  const rawId = url.pathname.split("/")[4];
  const parsed = IdSchema.safeParse(rawId);
  if (!parsed.success) return json({ error: "Invalid invite ID" }, 400);

  const invite = db
    .query(
      "SELECT * FROM game_invites WHERE id = ? AND to_user_id = ? AND status = 'pending'",
    )
    .get(parsed.data, auth.user_id) as GameInviteRow | null;

  if (!invite)
    return json({ error: "Invite not found or already resolved" }, 404);

  db.query("UPDATE game_invites SET status = 'declined' WHERE id = ?").run(
    invite.id,
  );

  pushToUser(invite.from_user_id, {
    type: "invite_declined",
    invite_id: invite.id,
    by: { id: auth.user_id, username: auth.username },
  });

  return json({ message: "Game invite declined." });
}

export async function handleCancelGameInvite(req: Request): Promise<Response> {
  const auth = await requireAuth(req);
  if (auth instanceof Response) return auth;

  const url = new URL(req.url);
  const rawId = url.pathname.split("/")[4];
  const parsed = IdSchema.safeParse(rawId);
  if (!parsed.success) return json({ error: "Invalid invite ID" }, 400);

  const invite = db
    .query(
      "SELECT * FROM game_invites WHERE id = ? AND from_user_id = ? AND status = 'pending'",
    )
    .get(parsed.data, auth.user_id) as GameInviteRow | null;

  if (!invite)
    return json({ error: "Invite not found or already resolved" }, 404);

  db.query("UPDATE game_invites SET status = 'cancelled' WHERE id = ?").run(
    invite.id,
  );

  pushToUser(invite.to_user_id, {
    type: "invite_cancelled",
    invite_id: invite.id,
  });

  return json({ message: "Game invite cancelled." });
}
