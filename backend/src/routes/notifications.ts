import { z } from "zod";
import { db } from "../db/database";
import { requireAuth } from "../middleware/auth";
import { json } from "../util/response";
import type { NotificationCategory, NotificationRow, NotificationResponse } from "../model/notification";

export function createNotification(
  userId: number,
  category: NotificationCategory,
  title: string,
  body: string,
): void {
  db.query(
    "INSERT INTO notifications (user_id, category, title, body) VALUES (?, ?, ?, ?)",
  ).run(userId, category, title, body);
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
