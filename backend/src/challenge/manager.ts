import type { ServerWebSocket } from "bun";
import { db } from "../db/database";
import type { WsData } from "../util/websocket";
import { buildMatchContent } from "../match/content";
import { participantFor, type Participant } from "../match/types";
import { creditReward, hasStake, refundStake, spendStake } from "./stakes";
import {
  MAX_CHALLENGE_PARTICIPANTS,
  MAX_STAKE_STARS,
  MAX_STAKE_TOKENS,
  MIN_PARTICIPANTS_TO_MANUAL_START,
  type ChallengeClientMsg,
  type ChallengeDto,
  type ChallengeStatus,
} from "./types";

interface ActiveChallenge {
  id: string;
  creatorId: number;
  stakeStars: number;
  stakeTokens: number;
  status: "open" | "active";
  /** Map čuva redosled prijavljivanja — koristi se kao tie-break pri rangiranju. */
  participants: Map<number, Participant>;
  results: Map<number, number>;
}

const active = new Map<string, ActiveChallenge>();

function send(ws: ServerWebSocket<WsData>, payload: object): void {
  try {
    ws.send(JSON.stringify(payload));
  } catch {
    // Socket je možda već zatvoren — ignoriši.
  }
}

function sendError(ws: ServerWebSocket<WsData>, message: string): void {
  send(ws, { type: "error", message });
}

function broadcast(c: ActiveChallenge, payload: object): void {
  for (const p of c.participants.values()) send(p.ws, payload);
}

function toDto(c: ActiveChallenge): ChallengeDto {
  const creator = c.participants.get(c.creatorId);
  return {
    id: c.id,
    creatorId: c.creatorId,
    creatorUsername: creator?.username ?? "",
    stakeStars: c.stakeStars,
    stakeTokens: c.stakeTokens,
    status: c.status,
    participants: [...c.participants.values()].map((p) => ({
      userId: p.userId as number,
      username: p.username,
      score: c.results.get(p.userId as number) ?? null,
      rewardStars: null,
      rewardTokens: null,
    })),
  };
}

function broadcastUpdate(c: ActiveChallenge): void {
  broadcast(c, { type: "challenge_update", challenge: toDto(c) });
}

export function onSocketMessage(ws: ServerWebSocket<WsData>, raw: string | Buffer): void {
  const text = typeof raw === "string" ? raw : raw.toString();
  let msg: ChallengeClientMsg;
  try {
    msg = JSON.parse(text);
  } catch {
    return;
  }

  switch (msg.type) {
    case "create_challenge":
      handleCreate(ws, msg.stars, msg.tokens);
      break;
    case "join_challenge":
      handleJoin(ws, msg.challengeId);
      break;
    case "leave_challenge":
      handleLeave(ws, msg.challengeId);
      break;
    case "start_challenge":
      handleStart(ws, msg.challengeId);
      break;
    case "report_challenge_result":
      handleReport(ws, msg.challengeId, msg.score);
      break;
  }
}

export function onSocketClose(ws: ServerWebSocket<WsData>): void {
  if (ws.data.kind !== "user") return;
  const userId = ws.data.userId;
  for (const c of [...active.values()]) {
    if (c.status === "open" && c.participants.get(userId)?.ws === ws) {
      leaveOpenChallenge(c, userId);
    }
  }
}

function handleCreate(ws: ServerWebSocket<WsData>, stars: number, tokens: number): void {
  const participant = participantFor(ws);
  if (participant.userId === null) {
    sendError(ws, "Gost ne može da pokrene izazov.");
    return;
  }
  if (!Number.isInteger(stars) || !Number.isInteger(tokens) || stars < 0 || tokens < 0 ||
    stars > MAX_STAKE_STARS || tokens > MAX_STAKE_TOKENS) {
    sendError(ws, `Ulog mora biti 0-${MAX_STAKE_STARS} zvezda i 0-${MAX_STAKE_TOKENS} tokena.`);
    return;
  }
  if (!spendStake(participant.userId, stars, tokens)) {
    sendError(ws, "Nemate dovoljno zvezda/tokena za ovaj ulog.");
    return;
  }

  const id = crypto.randomUUID();
  const c: ActiveChallenge = {
    id,
    creatorId: participant.userId,
    stakeStars: stars,
    stakeTokens: tokens,
    status: "open",
    participants: new Map([[participant.userId, participant]]),
    results: new Map(),
  };
  active.set(id, c);

  db.query(
    "INSERT INTO challenges (id, creator_id, stake_stars, stake_tokens, status) VALUES (?, ?, ?, ?, 'open')",
  ).run(id, participant.userId, stars, tokens);
  db.query("INSERT INTO challenge_participants (challenge_id, user_id) VALUES (?, ?)").run(
    id,
    participant.userId,
  );

  send(ws, { type: "challenge_created", challengeId: id });
  broadcastUpdate(c);
}

function handleJoin(ws: ServerWebSocket<WsData>, challengeId: string): void {
  const c = active.get(challengeId);
  if (!c) {
    sendError(ws, "Izazov ne postoji ili je završen.");
    return;
  }
  const participant = participantFor(ws);
  if (participant.userId === null) {
    sendError(ws, "Gost ne može da se priključi izazovu.");
    return;
  }

  // Idempotentno: igrač koji se već priključio (uklj. domaćina) samo dobija trenutno stanje.
  if (c.participants.has(participant.userId)) {
    c.participants.set(participant.userId, participant); // osveži ws (npr. reconnect)
    send(ws, { type: "challenge_update", challenge: toDto(c) });
    return;
  }

  if (c.status !== "open") {
    sendError(ws, "Izazov je već počeo.");
    return;
  }
  if (c.participants.size >= MAX_CHALLENGE_PARTICIPANTS) {
    sendError(ws, "Izazov je popunjen.");
    return;
  }
  if (!spendStake(participant.userId, c.stakeStars, c.stakeTokens)) {
    sendError(ws, "Nemate dovoljno zvezda/tokena za ovaj ulog.");
    return;
  }

  c.participants.set(participant.userId, participant);
  db.query("INSERT INTO challenge_participants (challenge_id, user_id) VALUES (?, ?)").run(
    challengeId,
    participant.userId,
  );

  broadcastUpdate(c);

  if (c.participants.size >= MAX_CHALLENGE_PARTICIPANTS) {
    startChallenge(c);
  }
}

function handleLeave(ws: ServerWebSocket<WsData>, challengeId: string): void {
  const c = active.get(challengeId);
  if (!c || c.status !== "open") return;
  const participant = participantFor(ws);
  if (participant.userId === null) return;
  leaveOpenChallenge(c, participant.userId);
}

function leaveOpenChallenge(c: ActiveChallenge, userId: number): void {
  if (!c.participants.has(userId)) return;

  if (userId === c.creatorId) {
    // Domaćin napušta pre starta — otkazuje izazov, svi dobijaju ulog nazad.
    for (const p of c.participants.values()) {
      if (p.userId !== null) refundStake(p.userId, c.stakeStars, c.stakeTokens);
    }
    db.query("UPDATE challenges SET status = 'cancelled' WHERE id = ?").run(c.id);
    db.query("DELETE FROM challenge_participants WHERE challenge_id = ?").run(c.id);
    broadcast(c, { type: "challenge_cancelled", challengeId: c.id });
    active.delete(c.id);
    return;
  }

  refundStake(userId, c.stakeStars, c.stakeTokens);
  c.participants.delete(userId);
  db.query("DELETE FROM challenge_participants WHERE challenge_id = ? AND user_id = ?").run(
    c.id,
    userId,
  );
  broadcastUpdate(c);
}

function handleStart(ws: ServerWebSocket<WsData>, challengeId: string): void {
  const c = active.get(challengeId);
  if (!c || c.status !== "open") return;
  const participant = participantFor(ws);
  if (participant.userId !== c.creatorId) {
    sendError(ws, "Samo domaćin može da pokrene izazov.");
    return;
  }
  if (c.participants.size < MIN_PARTICIPANTS_TO_MANUAL_START) {
    sendError(ws, "Potrebno je još učesnika.");
    return;
  }
  startChallenge(c);
}

function startChallenge(c: ActiveChallenge): void {
  c.status = "active";
  db.query("UPDATE challenges SET status = 'active', started_at = unixepoch() WHERE id = ?").run(c.id);

  const content = buildMatchContent();
  broadcast(c, { type: "challenge_started", challengeId: c.id, content });
}

function handleReport(ws: ServerWebSocket<WsData>, challengeId: string, score: number): void {
  const c = active.get(challengeId);
  if (!c || c.status !== "active") return;
  const participant = participantFor(ws);
  if (participant.userId === null || !c.participants.has(participant.userId)) return;
  if (c.results.has(participant.userId)) return; // već prijavljeno

  const safeScore = Math.max(0, Math.trunc(score));
  c.results.set(participant.userId, safeScore);
  db.query("UPDATE challenge_participants SET score = ? WHERE challenge_id = ? AND user_id = ?").run(
    safeScore,
    c.id,
    participant.userId,
  );

  if (c.results.size >= c.participants.size) {
    finishChallenge(c);
  }
}

function finishChallenge(c: ActiveChallenge): void {
  const ranking = [...c.participants.values()].sort(
    (a, b) => (c.results.get(b.userId as number) ?? 0) - (c.results.get(a.userId as number) ?? 0),
  );

  const n = ranking.length;
  const totalStars = c.stakeStars * n;
  const totalTokens = c.stakeTokens * n;
  const winnerStars = Math.floor(totalStars * 0.75);
  const winnerTokens = Math.floor(totalTokens * 0.75);

  const status: ChallengeStatus = "finished";
  db.query("UPDATE challenges SET status = ?, finished_at = unixepoch() WHERE id = ?").run(status, c.id);

  const results = ranking.map((p, rank) => {
    const userId = p.userId as number;
    let rewardStars = 0;
    let rewardTokens = 0;
    if (rank === 0) {
      rewardStars = winnerStars;
      rewardTokens = winnerTokens;
      creditReward(userId, rewardStars, rewardTokens);
    } else if (rank === 1) {
      // „Dobija nazad uloženo" — vraća tačno svoj ulog (spec 9.d).
      rewardStars = c.stakeStars;
      rewardTokens = c.stakeTokens;
      refundStake(userId, rewardStars, rewardTokens);
    }

    db.query(
      "UPDATE challenge_participants SET reward_stars = ?, reward_tokens = ? WHERE challenge_id = ? AND user_id = ?",
    ).run(rewardStars, rewardTokens, c.id, userId);

    return {
      userId,
      username: p.username,
      score: c.results.get(userId) ?? 0,
      rewardStars,
      rewardTokens,
    };
  });

  broadcast(c, { type: "challenge_over", challengeId: c.id, results });
  active.delete(c.id);
}
