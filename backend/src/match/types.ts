import type { ServerWebSocket } from "bun";
import type { WsData } from "../util/websocket";

/** Učesnik partije/matchmakinga. userId === null znači gost (bez nagrada). */
export interface Participant {
  ws: ServerWebSocket<WsData>;
  userId: number | null;
  username: string;
}

export type PerGameStats = {
  game: string;
  statistics: Record<string, unknown>;
};

/** Poruke koje klijent šalje serveru (ključevi su camelCase, kao na klijentu). */
export type ClientMsg =
  | { type: "find_match"; mode?: string }
  | { type: "cancel_find" }
  | { type: "match_move"; matchId: string; gameIndex: number; action: string; payload: Record<string, unknown> }
  | { type: "report_result"; matchId: string; blueScore: number; redScore: number; perGame: PerGameStats[] }
  | { type: "leave_match"; matchId: string };

/** Nagrade za jednog registrovanog igrača na kraju partije (spec 3.d). */
export interface MatchRewards {
  won: boolean;
  starsDelta: number;
  tokensDelta: number;
  totalStars: number;
  tokens: number;
  league: string;
}

export function participantFor(ws: ServerWebSocket<WsData>): Participant {
  if (ws.data.kind === "user") {
    return { ws, userId: ws.data.userId, username: ws.data.username };
  }
  return { ws, userId: null, username: ws.data.username };
}
