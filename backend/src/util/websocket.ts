import type { ServerWebSocket } from "bun";

/**
 * Identitet socket konekcije. Registrovan korisnik ima userId; gost ima privremeni guestId
 * (nema userId, ne ulazi u presence/prijatelje, ne dobija nagrade).
 */
export type WsData =
  | { kind: "user"; userId: number; username: string }
  | { kind: "guest"; guestId: string; username: string };

const connections = new Map<number, Set<ServerWebSocket<WsData>>>();

export function isOnline(userId: number): boolean {
  const set = connections.get(userId);
  return !!set && set.size > 0;
}

/** Vraća jedan aktivni socket korisnika (za pokretanje prijateljske partije), ili null. */
export function getSocket(userId: number): ServerWebSocket<WsData> | null {
  const set = connections.get(userId);
  if (!set) return null;
  for (const ws of set) return ws;
  return null;
}

/** Id-jevi svih trenutno povezanih (online) registrovanih korisnika — za statistiku regiona. */
export function onlineUserIds(): number[] {
  return [...connections.keys()];
}

export function pushToUser(userId: number, payload: object): void {
  const set = connections.get(userId);
  if (!set) return;
  const msg = JSON.stringify(payload);
  for (const ws of set) {
    ws.send(msg);
  }
}

export function registerConnection(userId: number, ws: ServerWebSocket<WsData>): void {
  if (!connections.has(userId)) {
    connections.set(userId, new Set());
  }
  connections.get(userId)!.add(ws);
}

export function removeConnection(userId: number, ws: ServerWebSocket<WsData>): void {
  const set = connections.get(userId);
  if (!set) return;
  set.delete(ws);
  if (set.size === 0) connections.delete(userId);
}
