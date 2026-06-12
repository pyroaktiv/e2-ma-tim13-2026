// Matchmaking queue + routing of all in-game WebSocket messages.
// This is the single entry point used by the WebSocket handler in index.ts.

import { db } from "../db/database";
import { isOnline, pushToUser } from "../util/websocket";
import { Match, type MatchPlayer } from "./match";
import type { GameMessage } from "./types";

const quickQueue: number[] = [];
const userToMatch = new Map<number, Match>();

function usernameOf(userId: number): string {
  const row = db
    .query("SELECT username FROM users WHERE id = ?")
    .get(userId) as { username: string } | null;
  return row?.username ?? `Player ${userId}`;
}

function setInGame(userId: number, value: boolean): void {
  db.query("UPDATE users SET in_game = ? WHERE id = ?").run(value ? 1 : 0, userId);
}

// Spec 3.a: one token starts one partija. Quick-match partije are ranked, so we
// spend a token from each player (clamped at 0) when the match begins.
function spendToken(userId: number): void {
  db.query(
    "UPDATE users SET tokens = MAX(0, tokens - 1) WHERE id = ?",
  ).run(userId);
}

function startMatch(a: number, b: number): void {
  const p0: MatchPlayer = { userId: a, username: usernameOf(a) };
  const p1: MatchPlayer = { userId: b, username: usernameOf(b) };

  spendToken(a);
  spendToken(b);

  const match = new Match(crypto.randomUUID(), p0, p1, (m) => {
    // cleanup when the match ends
    for (const id of m.userIds) {
      if (userToMatch.get(id) === m) userToMatch.delete(id);
      setInGame(id, false);
    }
  });

  userToMatch.set(a, match);
  userToMatch.set(b, match);
  setInGame(a, true);
  setInGame(b, true);
  match.start();
}

function enqueueQuickMatch(userId: number): void {
  if (userToMatch.has(userId)) {
    pushToUser(userId, { type: "error", message: "Already in a match" });
    return;
  }
  if (quickQueue.includes(userId)) return;

  // find a waiting opponent that is still online and free
  while (quickQueue.length > 0) {
    const candidate = quickQueue.shift()!;
    if (candidate === userId) continue;
    if (!isOnline(candidate) || userToMatch.has(candidate)) continue;
    startMatch(candidate, userId);
    return;
  }

  quickQueue.push(userId);
  pushToUser(userId, { type: "queued" });
}

function dequeue(userId: number): void {
  const idx = quickQueue.indexOf(userId);
  if (idx !== -1) quickQueue.splice(idx, 1);
}

export function handleGameMessage(userId: number, raw: string): void {
  let msg: GameMessage;
  try {
    msg = JSON.parse(raw);
  } catch {
    return;
  }
  if (!msg || typeof msg.type !== "string") return;

  switch (msg.type) {
    case "quick_match":
      enqueueQuickMatch(userId);
      return;
    case "cancel_match":
      dequeue(userId);
      pushToUser(userId, { type: "match_cancelled" });
      return;
    case "leave_match": {
      dequeue(userId);
      const match = userToMatch.get(userId);
      if (match) match.handleLeave(userId);
      return;
    }
    default: {
      const match = userToMatch.get(userId);
      if (match) {
        try {
          match.handleMessage(userId, msg);
        } catch (err) {
          console.error(`[game] handleMessage error (user ${userId}, type ${msg.type}):`, err);
        }
      }
      return;
    }
  }
}

// Called when a user has no remaining WebSocket connections.
export function handleDisconnect(userId: number): void {
  dequeue(userId);
  const match = userToMatch.get(userId);
  if (match) match.handleLeave(userId);
}
