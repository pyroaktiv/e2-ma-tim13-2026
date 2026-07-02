import { z } from "zod";
import { db } from "../db/database";
import { requireAuth } from "../middleware/auth";
import { json } from "../util/response";
import { isOnline, pushToUser } from "../util/websocket";
import { sendFcmToUser } from "../util/fcm";
import type { NotificationCategory, NotificationRow, NotificationResponse } from "../model/notification";

/**
 * Centralna tačka za sistemske notifikacije (spec 11). Uvek upisuje u istoriju.
 * Ako je korisnik povezan (u aplikaciji) — šalje WS `notification` event radi
 * uživo osvežavanja liste; u suprotnom šalje FCM push da stigne i van aplikacije.
 */
export function createNotification(
  userId: number,
  category: NotificationCategory,
  title: string,
  body: string,
): void {
  const res = db
    .query(
      "INSERT INTO notifications (user_id, category, title, body) VALUES (?, ?, ?, ?)",
    )
    .run(userId, category, title, body);

  const payload = {
    type: "notification",
    id: Number(res.lastInsertRowid),
    category,
    title,
    body,
    timestamp: new Date().toISOString(),
    is_read: false,
  };

  if (isOnline(userId)) {
    pushToUser(userId, payload);
  } else {
    // Fire-and-forget: ne blokira pozivaoce (createNotification je sinhron).
    void sendFcmToUser(userId, category, title, body).catch((e) =>
      console.warn("[notifications] FCM slanje nije uspelo:", e),
    );
  }
}

const FcmTokenSchema = z.object({ token: z.string().min(1).max(4096) });

export async function handleRegisterFcmToken(req: Request): Promise<Response> {
  const auth = await requireAuth(req);
  if (auth instanceof Response) return auth;

  let body: unknown;
  try {
    body = await req.json();
  } catch {
    return json({ error: "Invalid JSON" }, 400);
  }

  const parsed = FcmTokenSchema.safeParse(body);
  if (!parsed.success) return json({ error: "Invalid token" }, 400);

  // Isti token može promeniti vlasnika (npr. novi login na istom uređaju) — upsert.
  db.query(
    `INSERT INTO fcm_tokens (token, user_id, updated_at) VALUES (?, ?, unixepoch())
     ON CONFLICT(token) DO UPDATE SET user_id = excluded.user_id, updated_at = unixepoch()`,
  ).run(parsed.data.token, auth.user_id);

  return json({ message: "FCM token registered." });
}

export async function handleUnregisterFcmToken(req: Request): Promise<Response> {
  const auth = await requireAuth(req);
  if (auth instanceof Response) return auth;

  let body: unknown;
  try {
    body = await req.json();
  } catch {
    return json({ error: "Invalid JSON" }, 400);
  }

  const parsed = FcmTokenSchema.safeParse(body);
  if (!parsed.success) return json({ error: "Invalid token" }, 400);

  db.query("DELETE FROM fcm_tokens WHERE token = ? AND user_id = ?").run(
    parsed.data.token,
    auth.user_id,
  );

  return json({ message: "FCM token removed." });
}

export async function handleGetNotifications(req: Request): Promise<Response> {
  const auth = await requireAuth(req);
  if (auth instanceof Response) return auth;

  const rows = db
    .query(
      `SELECT id, category, title, body, is_read, created_at
       FROM notifications
       WHERE user_id = ?
       ORDER BY created_at DESC`,
    )
    .all(auth.user_id) as NotificationRow[];

  const notifications: NotificationResponse[] = rows.map((row) => ({
    id: row.id,
    category: row.category,
    title: row.title,
    body: row.body,
    timestamp: new Date(row.created_at * 1000).toISOString(),
    is_read: row.is_read === 1,
  }));

  return json(notifications);
}

const IdSchema = z.coerce.number().int().positive();

export async function handleMarkAsRead(req: Request): Promise<Response> {
  const auth = await requireAuth(req);
  if (auth instanceof Response) return auth;

  const url = new URL(req.url);
  const rawId = url.pathname.split("/")[3];

  const parsed = IdSchema.safeParse(rawId);
  if (!parsed.success) {
    return json({ error: "Invalid notification ID" }, 400);
  }

  const id = parsed.data;

  const existing = db
    .query("SELECT id FROM notifications WHERE id = ? AND user_id = ?")
    .get(id, auth.user_id) as { id: number } | null;

  if (!existing) {
    return json({ error: "Notification not found" }, 404);
  }

  db.query("UPDATE notifications SET is_read = 1 WHERE id = ?").run(id);

  return json({ message: "Notification marked as read." });
}
