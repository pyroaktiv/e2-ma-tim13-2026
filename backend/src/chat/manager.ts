import type { ServerWebSocket } from "bun";
import { db } from "../db/database";
import { isOnline, pushToUser, type WsData } from "../util/websocket";
import { createNotification } from "../routes/notifications";
import { progressMission } from "../util/missions";

const MAX_BODY_LENGTH = 1000;

interface ChatSendMsg {
  type: "chat_send";
  toUserId: number;
  body: string;
}

/** Realtime slanje poruka (spec 8); čuva poruku, isporučuje uživo ili kreira notifikaciju (spec 11). */
export function onSocketMessage(ws: ServerWebSocket<WsData>, raw: string | Buffer): void {
  if (ws.data.kind !== "user") return;

  let parsed: unknown;
  try {
    parsed = JSON.parse(raw.toString());
  } catch {
    return;
  }
  if (typeof parsed !== "object" || parsed === null || (parsed as { type?: unknown }).type !== "chat_send") {
    return;
  }

  const msg = parsed as ChatSendMsg;
  const fromUserId = ws.data.userId;
  const toUserId = Number(msg.toUserId);
  const body = typeof msg.body === "string" ? msg.body.trim() : "";

  if (!Number.isInteger(toUserId) || toUserId <= 0 || fromUserId === toUserId) return;
  if (!body || body.length > MAX_BODY_LENGTH) {
    pushToUser(fromUserId, { type: "error", message: "Poruka je prazna ili previše duga." });
    return;
  }

  const users = db
    .query("SELECT id, username, region FROM users WHERE id IN (?, ?)")
    .all(fromUserId, toUserId) as Array<{ id: number; username: string; region: string }>;
  const from = users.find((u) => u.id === fromUserId);
  const to = users.find((u) => u.id === toUserId);

  if (!from || !to) {
    pushToUser(fromUserId, { type: "error", message: "Korisnik ne postoji." });
    return;
  }
  if (from.region !== to.region) {
    pushToUser(fromUserId, { type: "error", message: "Čet je dostupan samo unutar istog regiona." });
    return;
  }

  const insertResult = db
    .query("INSERT INTO chat_messages (from_user_id, to_user_id, body) VALUES (?, ?, ?)")
    .run(fromUserId, toUserId, body);

  const row = db
    .query("SELECT created_at FROM chat_messages WHERE id = ?")
    .get(Number(insertResult.lastInsertRowid)) as { created_at: number };

  const payload = {
    type: "chat_message",
    id: Number(insertResult.lastInsertRowid),
    fromUserId,
    fromUsername: from.username,
    toUserId,
    body,
    createdAt: new Date(row.created_at * 1000).toISOString(),
  };

  pushToUser(fromUserId, payload);

  if (isOnline(toUserId)) {
    pushToUser(toUserId, payload);
  } else {
    createNotification(toUserId, "CET", from.username, body.slice(0, 120));
  }

  progressMission(fromUserId, "send_chat");
}
