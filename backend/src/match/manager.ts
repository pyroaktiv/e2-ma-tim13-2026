import type { ServerWebSocket } from "bun";
import { db } from "../db/database";
import type { WsData } from "../util/websocket";
import { hasToken, refundMatchToken, spendMatchToken } from "../util/tokens";
import { buildMatchContent } from "./content";
import { applyMatchOutcome } from "./rewards";
import { participantFor, type ClientMsg, type Participant } from "./types";

interface Room {
  id: string;
  blue: Participant;
  red: Participant;
  finished: boolean;
}

const queue: Participant[] = [];
const rooms = new Map<string, Room>();
const wsRoom = new Map<ServerWebSocket<WsData>, string>();

function send(ws: ServerWebSocket<WsData>, payload: object): void {
  try {
    ws.send(JSON.stringify(payload));
  } catch {
    // Socket je možda već zatvoren (igrač napustio) — ignoriši.
  }
}

function sendError(ws: ServerWebSocket<WsData>, message: string): void {
  send(ws, { type: "error", message });
}

function setInGame(p: Participant, value: boolean): void {
  if (p.userId !== null) {
    db.query("UPDATE users SET in_game = ? WHERE id = ?").run(value ? 1 : 0, p.userId);
  }
}

export function onSocketMessage(ws: ServerWebSocket<WsData>, raw: string | Buffer): void {
  const text = typeof raw === "string" ? raw : raw.toString();
  let msg: ClientMsg;
  try {
    msg = JSON.parse(text);
  } catch {
    return;
  }

  switch (msg.type) {
    case "find_match":
      handleFindMatch(ws);
      break;
    case "cancel_find":
      removeFromQueue(ws);
      break;
    case "match_move":
      handleMatchMove(ws, msg.gameIndex, msg.action, msg.payload);
      break;
    case "report_result":
      handleReport(ws, msg.blueScore, msg.redScore);
      break;
    case "leave_match":
      handleLeave(ws);
      break;
  }
}

export function onSocketClose(ws: ServerWebSocket<WsData>): void {
  removeFromQueue(ws);
  handleLeave(ws);
}

function handleFindMatch(ws: ServerWebSocket<WsData>): void {
  if (wsRoom.has(ws) || queue.some((p) => p.ws === ws)) return;

  const participant = participantFor(ws);
  if (participant.userId !== null && !hasToken(participant.userId)) {
    sendError(ws, "Nemate dovoljno tokena za partiju.");
    return;
  }

  const opponent = queue.shift();
  if (opponent) {
    createRoom(opponent, participant);
  } else {
    queue.push(participant);
  }
}

function createRoom(a: Participant, b: Participant): void {
  // Naplata tokena za registrovane (spec 3.a). Gost ne troši token.
  if (a.userId !== null && !spendMatchToken(a.userId)) {
    sendError(a.ws, "Nemate dovoljno tokena za partiju.");
    queue.unshift(b);
    return;
  }
  if (b.userId !== null && !spendMatchToken(b.userId)) {
    sendError(b.ws, "Nemate dovoljno tokena za partiju.");
    if (a.userId !== null) refundMatchToken(a.userId);
    queue.unshift(a);
    return;
  }

  const id = crypto.randomUUID();
  const [blue, red] = Math.random() < 0.5 ? [a, b] : [b, a];
  const room: Room = { id, blue, red, finished: false };
  rooms.set(id, room);
  wsRoom.set(blue.ws, id);
  wsRoom.set(red.ws, id);
  setInGame(blue, true);
  setInGame(red, true);

  const content = buildMatchContent();
  send(blue.ws, {
    type: "match_found",
    matchId: id,
    color: "BLUE",
    opponent: { username: red.username, guest: red.userId === null },
    content,
  });
  send(red.ws, {
    type: "match_found",
    matchId: id,
    color: "RED",
    opponent: { username: blue.username, guest: blue.userId === null },
    content,
  });
}

function handleMatchMove(
  ws: ServerWebSocket<WsData>,
  gameIndex: number,
  action: string,
  payload: Record<string, unknown>,
): void {
  const room = roomOf(ws);
  if (!room || room.finished) return;
  const opponent = ws === room.blue.ws ? room.red : room.blue;
  send(opponent.ws, { type: "match_move", gameIndex, action, payload });
}

function handleReport(ws: ServerWebSocket<WsData>, blueScore: number, redScore: number): void {
  const room = roomOf(ws);
  if (!room || room.finished) return;
  room.finished = true;

  const winner = blueScore > redScore ? "blue" : blueScore < redScore ? "red" : "draw";

  const blueRewards = room.blue.userId !== null ? applyMatchOutcome(room.blue.userId, blueScore, redScore) : null;
  const redRewards = room.red.userId !== null ? applyMatchOutcome(room.red.userId, redScore, blueScore) : null;

  db.query(
    "INSERT INTO matches (blue_user_id, red_user_id, blue_score, red_score, winner) VALUES (?, ?, ?, ?, ?)",
  ).run(room.blue.userId, room.red.userId, blueScore, redScore, winner);

  send(room.blue.ws, { type: "match_over", rewards: blueRewards });
  send(room.red.ws, { type: "match_over", rewards: redRewards });

  cleanupRoom(room);
}

function handleLeave(ws: ServerWebSocket<WsData>): void {
  const room = roomOf(ws);
  if (!room) return;

  if (room.finished) {
    cleanupRoom(room);
    return;
  }

  // Napuštanjem igrač gubi partiju; protivnik nastavlja i prijaviće rezultat (spec 3.f).
  const opponent = ws === room.blue.ws ? room.red : room.blue;
  send(opponent.ws, { type: "opponent_left" });
  wsRoom.delete(ws);
  setInGame(ws === room.blue.ws ? room.blue : room.red, false);
}

function roomOf(ws: ServerWebSocket<WsData>): Room | undefined {
  const id = wsRoom.get(ws);
  return id ? rooms.get(id) : undefined;
}

function removeFromQueue(ws: ServerWebSocket<WsData>): void {
  const index = queue.findIndex((p) => p.ws === ws);
  if (index >= 0) queue.splice(index, 1);
}

function cleanupRoom(room: Room): void {
  rooms.delete(room.id);
  wsRoom.delete(room.blue.ws);
  wsRoom.delete(room.red.ws);
  setInGame(room.blue, false);
  setInGame(room.red, false);
}
