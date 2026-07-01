import type { ServerWebSocket } from "bun";
import { db } from "../db/database";
import { getSocket, type WsData } from "../util/websocket";
import { hasToken, refundMatchToken, spendMatchToken } from "../util/tokens";
import { buildMatchContent } from "./content";
import { applyMatchOutcome } from "./rewards";
import { applyPerGameStats } from "./stats";
import { participantFor, type ClientMsg, type Participant, type PerGameStats } from "./types";

interface Room {
  id: string;
  blue: Participant;
  red: Participant;
  finished: boolean;
  /** Rangirana partija troši token i daje zvezde/statistiku; prijateljska (spec 3.e) ne. */
  ranked?: boolean;
  // Tournament support (optional)
  tournamentId?: string;
  tournamentPhase?: "semi" | "final";
  semiIndex?: 0 | 1;
}

type TournamentRoomHandler = (
  tournamentId: string,
  roomId: string,
  phase: "semi" | "final",
  blueUserId: number | null,
  blueScore: number,
  redUserId: number | null,
  redScore: number,
) => void;

let _tournamentRoomHandler: TournamentRoomHandler | null = null;

export function setTournamentRoomHandler(fn: TournamentRoomHandler): void {
  _tournamentRoomHandler = fn;
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
      handleReport(ws, msg.blueScore, msg.redScore, msg.perGame);
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

function createRoom(a: Participant, b: Participant, ranked = true): void {
  // Naplata tokena za registrovane (spec 3.a). Gost ne troši token. Prijateljska partija
  // (spec 3.e) ne koristi tokene.
  if (ranked) {
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
  }

  const id = crypto.randomUUID();
  const [blue, red] = Math.random() < 0.5 ? [a, b] : [b, a];
  const room: Room = { id, blue, red, finished: false, ranked };
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

/**
 * Pokreće prijateljsku (nerangiranu) partiju između dva online korisnika (spec 7.d) kada
 * pozvani prihvati poziv. Vraća false ako neko nije online ili je već u partiji/redu.
 */
export function startFriendlyMatch(
  inviter: { id: number; username: string },
  invitee: { id: number; username: string },
): boolean {
  const wsA = getSocket(inviter.id);
  const wsB = getSocket(invitee.id);
  if (!wsA || !wsB) return false;
  if (wsRoom.has(wsA) || wsRoom.has(wsB)) return false;

  removeFromQueue(wsA);
  removeFromQueue(wsB);

  createRoom(
    { ws: wsA, userId: inviter.id, username: inviter.username },
    { ws: wsB, userId: invitee.id, username: invitee.username },
    false,
  );
  return true;
}

/**
 * Creates a room for a tournament match. Skips token deduction (already taken by tournament
 * manager). Does NOT send match_found — the tournament manager sends tournament_found instead.
 * Returns roomId, the color assigned to each participant, and the shared match content.
 */
export function createTournamentRoom(
  a: Participant,
  b: Participant,
  tournamentId: string,
  phase: "semi" | "final",
  semiIndex?: 0 | 1,
): { roomId: string; blueUserId: number | null; content: ReturnType<typeof buildMatchContent> } {
  const id = crypto.randomUUID();
  const [blue, red] = Math.random() < 0.5 ? [a, b] : [b, a];
  const room: Room = { id, blue, red, finished: false, tournamentId, tournamentPhase: phase, semiIndex };
  rooms.set(id, room);
  wsRoom.set(blue.ws, id);
  wsRoom.set(red.ws, id);
  setInGame(blue, true);
  setInGame(red, true);

  const content = buildMatchContent();
  return { roomId: id, blueUserId: blue.userId, content };
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

function handleReport(
  ws: ServerWebSocket<WsData>,
  blueScore: number,
  redScore: number,
  perGame?: PerGameStats[],
): void {
  const room = roomOf(ws);
  if (!room || room.finished) return;
  room.finished = true;

  // Turnirske partije imaju posebnu logiku nagrada — delegiramo turnir menadžeru.
  if (room.tournamentId && _tournamentRoomHandler) {
    _tournamentRoomHandler(
      room.tournamentId,
      room.id,
      room.tournamentPhase!,
      room.blue.userId,
      blueScore,
      room.red.userId,
      redScore,
    );
    cleanupRoom(room);
    return;
  }

  const winner = blueScore > redScore ? "blue" : blueScore < redScore ? "red" : "draw";

  // Prijateljska partija (spec 3.e): ne daje/uzima zvezde i tokene, ne ulazi u statistiku.
  const blueRewards =
    room.ranked && room.blue.userId !== null ? applyMatchOutcome(room.blue.userId, blueScore, redScore) : null;
  const redRewards =
    room.ranked && room.red.userId !== null ? applyMatchOutcome(room.red.userId, redScore, blueScore) : null;

  if (room.ranked) {
    // Per-game statistika profila (spec 2.c) — plavi doprinos plavom, crveni crvenom igraču.
    applyPerGameStats(room.blue.userId, room.red.userId, perGame);
  }

  db.query(
    "INSERT INTO matches (blue_user_id, red_user_id, blue_score, red_score, winner, is_ranked) VALUES (?, ?, ?, ?, ?, ?)",
  ).run(room.blue.userId, room.red.userId, blueScore, redScore, winner, room.ranked ? 1 : 0);

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
