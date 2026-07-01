import type { ServerWebSocket } from "bun";
import { db } from "../db/database";
import { createTournamentRoom, setTournamentRoomHandler } from "../match/manager";
import { participantFor, type Participant } from "../match/types";
import type { WsData } from "../util/websocket";
import { applyFinalLossRewards, applyFinalWinRewards, applySemiLossRewards, applySemiWinRewards } from "./rewards";
import type { ActiveTournament, TournamentPhase, TournamentRewards, TournamentSemiState } from "./types";

const tournamentQueue: Participant[] = [];
const tournaments = new Map<string, ActiveTournament>();

// Register this module's handler with match manager (avoids circular import).
setTournamentRoomHandler(onTournamentRoomFinished);

function send(ws: ServerWebSocket<WsData>, payload: object): void {
  try {
    ws.send(JSON.stringify(payload));
  } catch {
    // Socket zatvoren — ignoriši.
  }
}

function sendError(ws: ServerWebSocket<WsData>, message: string): void {
  send(ws, { type: "error", message });
}

function hasTournamentTokens(userId: number): boolean {
  const row = db.query("SELECT tokens FROM users WHERE id = ?").get(userId) as { tokens: number } | null;
  return !!row && row.tokens >= 3;
}

function spendTournamentTokens(userId: number): boolean {
  const result = db
    .query("UPDATE users SET tokens = tokens - 3 WHERE id = ? AND tokens >= 3")
    .run(userId);
  return result.changes > 0;
}

function refundTournamentTokens(userId: number): void {
  db.query("UPDATE users SET tokens = tokens + 3 WHERE id = ?").run(userId);
}

function isInQueue(ws: ServerWebSocket<WsData>): boolean {
  return tournamentQueue.some((p) => p.ws === ws);
}

function getBracketProfile(userId: number): { userId: number; username: string; avatar: string; league: { name: string; icon: string } } {
  const row = db.query(`
    SELECT u.id, u.username, u.avatar, l.name AS league_name, l.icon AS league_icon
    FROM users u JOIN leagues l ON l.id = u.league_id
    WHERE u.id = ?
  `).get(userId) as { id: number; username: string; avatar: string; league_name: string; league_icon: string } | null;

  return {
    userId: row?.id ?? userId,
    username: row?.username ?? "",
    avatar: row?.avatar ?? "default",
    league: { name: row?.league_name ?? "", icon: row?.league_icon ?? "" },
  };
}

export function onSocketMessage(ws: ServerWebSocket<WsData>, raw: string | Buffer): void {
  const text = typeof raw === "string" ? raw : raw.toString();
  let msg: { type: string };
  try {
    msg = JSON.parse(text);
  } catch {
    return;
  }

  switch (msg.type) {
    case "find_tournament":
      handleFindTournament(ws);
      break;
    case "cancel_tournament":
      handleCancelTournament(ws);
      break;
  }
}

export function onSocketClose(ws: ServerWebSocket<WsData>): void {
  removeFromQueue(ws, true);
}

function removeFromQueue(ws: ServerWebSocket<WsData>, refund: boolean): boolean {
  const index = tournamentQueue.findIndex((p) => p.ws === ws);
  if (index < 0) return false;
  const [p] = tournamentQueue.splice(index, 1);
  if (refund && p.userId !== null) refundTournamentTokens(p.userId);
  return true;
}

function handleFindTournament(ws: ServerWebSocket<WsData>): void {
  if (ws.data.kind !== "user") {
    sendError(ws, "Gost ne može da učestvuje u turniru.");
    return;
  }
  if (isInQueue(ws)) {
    sendError(ws, "Već ste u redu za turnir.");
    return;
  }

  const userId = ws.data.userId;
  if (!hasTournamentTokens(userId)) {
    sendError(ws, "Potrebno je 3 žetona za učestvovanje u turniru.");
    return;
  }
  if (!spendTournamentTokens(userId)) {
    sendError(ws, "Nemate dovoljno žetona za turnir.");
    return;
  }

  const participant = participantFor(ws);
  tournamentQueue.push(participant);
  send(ws, { type: "tournament_queued" });

  if (tournamentQueue.length >= 4) {
    const players = tournamentQueue.splice(0, 4);
    createTournament(players);
  }
}

function handleCancelTournament(ws: ServerWebSocket<WsData>): void {
  if (removeFromQueue(ws, true)) {
    send(ws, { type: "tournament_cancelled" });
  }
}

function createTournament(players: Participant[]): void {
  const id = crypto.randomUUID();

  db.query("INSERT INTO tournaments (id, status) VALUES (?, 'semifinal')").run(id);
  for (let i = 0; i < players.length; i++) {
    if (players[i].userId !== null) {
      db.query(
        "INSERT INTO tournament_participants (tournament_id, user_id, semi_index) VALUES (?, ?, ?)",
      ).run(id, players[i].userId, i < 2 ? 0 : 1);
    }
  }

  // Semi 0: players[0] vs players[1], Semi 1: players[2] vs players[3]
  const semi0 = buildSemiRoom(players[0], players[1], id, "semi", 0);
  const semi1 = buildSemiRoom(players[2], players[3], id, "semi", 1);

  db.query("UPDATE tournaments SET semi1_room_id = ?, semi2_room_id = ? WHERE id = ?").run(
    semi0.state.roomId,
    semi1.state.roomId,
    id,
  );

  const tournament: ActiveTournament = {
    id,
    status: "semifinal",
    allParticipants: [...players],
    semis: [semi0.state, semi1.state],
    finalRoomId: null,
  };
  tournaments.set(id, tournament);

  // Build bracket info for client display (avatar, league for all 4 players).
  const bracket = players.map((p) => (p.userId !== null ? getBracketProfile(p.userId) : {
    userId: null,
    username: p.username,
    avatar: "default",
    league: { name: "", icon: "" },
  }));

  // Notify each player with their specific semi match details.
  for (let i = 0; i < 4; i++) {
    const p = players[i];
    const semiIndex = i < 2 ? 0 : 1;
    const semi = semiIndex === 0 ? semi0 : semi1;
    const color = p.userId === semi.blueUserId ? "BLUE" : "RED";
    const opponent = semiIndex === 0
      ? (i === 0 ? players[1] : players[0])
      : (i === 2 ? players[3] : players[2]);

    send(p.ws, {
      type: "tournament_found",
      tournamentId: id,
      bracket,
      semiIndex,
      matchId: semi.state.roomId,
      color,
      opponent: { username: opponent.username },
      content: semi.content,
    });
  }
}

function buildSemiRoom(
  a: Participant,
  b: Participant,
  tournamentId: string,
  phase: TournamentPhase,
  semiIndex: 0 | 1,
): { state: TournamentSemiState; blueUserId: number | null; content: object } {
  const { roomId, blueUserId, content } = createTournamentRoom(a, b, tournamentId, phase, semiIndex);
  const [blue, red] = blueUserId === a.userId ? [a, b] : [b, a];
  return {
    state: {
      roomId,
      players: [blue, red],
      winnerId: null,
      winnerScore: 0,
      loserId: null,
      loserScore: 0,
      done: false,
    },
    blueUserId,
    content,
  };
}

export function onTournamentRoomFinished(
  tournamentId: string,
  roomId: string,
  phase: TournamentPhase,
  blueUserId: number | null,
  blueScore: number,
  redUserId: number | null,
  redScore: number,
): void {
  const tournament = tournaments.get(tournamentId);
  if (!tournament) return;

  if (phase === "semi") {
    handleSemiFinished(tournament, roomId, blueUserId, blueScore, redUserId, redScore);
  } else {
    handleFinalFinished(tournament, blueUserId, blueScore, redUserId, redScore);
  }
}

function handleSemiFinished(
  tournament: ActiveTournament,
  roomId: string,
  blueUserId: number | null,
  blueScore: number,
  redUserId: number | null,
  redScore: number,
): void {
  const semiIndex = tournament.semis[0].roomId === roomId ? 0 : 1;
  const semi = tournament.semis[semiIndex];
  if (semi.done) return;

  const winnerId = blueScore >= redScore ? blueUserId : redUserId;
  const winnerScore = blueScore >= redScore ? blueScore : redScore;
  const loserId = blueScore >= redScore ? redUserId : blueUserId;
  const loserScore = blueScore >= redScore ? redScore : blueScore;

  semi.winnerId = winnerId;
  semi.winnerScore = winnerScore;
  semi.loserId = loserId;
  semi.loserScore = loserScore;
  semi.done = true;

  // Apply rewards.
  const winnerRewards = winnerId !== null ? applySemiWinRewards(winnerId, winnerScore) : null;
  applySemiLossRewards(loserId ?? -1);

  // Persist results.
  if (winnerId !== null) {
    db.query(
      "UPDATE tournament_participants SET result = 'won', score = ? WHERE tournament_id = ? AND user_id = ?",
    ).run(winnerScore, tournament.id, winnerId);
  }
  if (loserId !== null) {
    db.query(
      "UPDATE tournament_participants SET result = 'lost', score = ? WHERE tournament_id = ? AND user_id = ?",
    ).run(loserScore, tournament.id, loserId);
  }

  // Find winner/loser Participant objects for message sending.
  const winnerParticipant = semi.players.find((p) => p.userId === winnerId);
  const loserParticipant = semi.players.find((p) => p.userId === loserId);

  const winnerInfo = { userId: winnerId, username: winnerParticipant?.username ?? "", score: winnerScore };
  const loserInfo = { userId: loserId, username: loserParticipant?.username ?? "", score: loserScore };

  // Notify both semi players individually with their own rewards.
  if (winnerParticipant) {
    send(winnerParticipant.ws, {
      type: "tournament_semi_over",
      tournamentId: tournament.id,
      semiIndex,
      winner: winnerInfo,
      loser: loserInfo,
      rewards: winnerRewards,
      won: true,
    });
  }
  if (loserParticipant) {
    send(loserParticipant.ws, {
      type: "tournament_semi_over",
      tournamentId: tournament.id,
      semiIndex,
      winner: winnerInfo,
      loser: loserInfo,
      rewards: null,
      won: false,
    });
  }

  // If both semis are done, advance to final.
  if (tournament.semis[0].done && tournament.semis[1].done) {
    startFinal(tournament);
  }
}

function startFinal(tournament: ActiveTournament): void {
  const [semi0, semi1] = tournament.semis;

  // Locate winner Participant objects from allParticipants.
  const winner0 = tournament.allParticipants.find((p) => p.userId === semi0.winnerId);
  const winner1 = tournament.allParticipants.find((p) => p.userId === semi1.winnerId);

  if (!winner0 || !winner1) return;

  const { roomId, blueUserId, content } = createTournamentRoom(winner0, winner1, tournament.id, "final");
  tournament.finalRoomId = roomId;
  tournament.status = "final";

  db.query("UPDATE tournaments SET status = 'final', final_room_id = ? WHERE id = ?").run(roomId, tournament.id);

  // Notify finalists.
  const color0 = blueUserId === winner0.userId ? "BLUE" : "RED";
  const color1 = blueUserId === winner0.userId ? "RED" : "BLUE";

  send(winner0.ws, {
    type: "tournament_final_started",
    tournamentId: tournament.id,
    matchId: roomId,
    color: color0,
    content,
    opponent: { username: winner1.username },
  });
  send(winner1.ws, {
    type: "tournament_final_started",
    tournamentId: tournament.id,
    matchId: roomId,
    color: color1,
    content,
    opponent: { username: winner0.username },
  });

  // Notify semi-losers so they can observe the bracket.
  const loser0 = tournament.allParticipants.find((p) => p.userId === semi0.loserId);
  const loser1 = tournament.allParticipants.find((p) => p.userId === semi1.loserId);

  const finalistInfo = [
    winner0.userId !== null ? getBracketProfile(winner0.userId) : null,
    winner1.userId !== null ? getBracketProfile(winner1.userId) : null,
  ].filter(Boolean);

  for (const loser of [loser0, loser1]) {
    if (loser) {
      send(loser.ws, {
        type: "tournament_update",
        tournamentId: tournament.id,
        status: "final",
        finalists: finalistInfo,
      });
    }
  }
}

function handleFinalFinished(
  tournament: ActiveTournament,
  blueUserId: number | null,
  blueScore: number,
  redUserId: number | null,
  redScore: number,
): void {
  if (tournament.status === "finished") return;
  tournament.status = "finished";

  const winnerId = blueScore >= redScore ? blueUserId : redUserId;
  const winnerScore = blueScore >= redScore ? blueScore : redScore;
  const loserId = blueScore >= redScore ? redUserId : blueUserId;
  const loserScore = blueScore >= redScore ? redScore : blueScore;

  const winnerRewards: TournamentRewards | null = winnerId !== null ? applyFinalWinRewards(winnerId, winnerScore) : null;
  const loserRewards: TournamentRewards | null = loserId !== null ? applyFinalLossRewards(loserId, loserScore) : null;

  db.query("UPDATE tournaments SET status = 'finished', finished_at = unixepoch() WHERE id = ?").run(tournament.id);

  const winnerParticipant = tournament.allParticipants.find((p) => p.userId === winnerId);
  const loserParticipant = tournament.allParticipants.find((p) => p.userId === loserId);

  const result = {
    type: "tournament_over",
    tournamentId: tournament.id,
    winner: {
      userId: winnerId,
      username: winnerParticipant?.username ?? "",
      score: winnerScore,
      rewards: winnerRewards,
    },
    runner_up: {
      userId: loserId,
      username: loserParticipant?.username ?? "",
      score: loserScore,
      rewards: loserRewards,
    },
  };

  // Broadcast final result to all 4 participants.
  for (const p of tournament.allParticipants) {
    send(p.ws, result);
  }

  tournaments.delete(tournament.id);
}
