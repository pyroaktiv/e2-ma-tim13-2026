// A match between two players. It plays a fixed sequence of games (for Student
// 2: Ko zna zna, then Spojnice), accumulates the total score across games and
// declares an overall winner, awarding stars per the spec.

import { pushToUser } from "../util/websocket";
import { createNotification } from "../routes/notifications";
import { KoZnaZnaGame } from "./koznazna";
import { SpojniceGame } from "./spojnice";
import { AsocijacijeGame } from "./asocijacije";
import { SkockoGame } from "./skocko";
import { KorakPoKorakGame } from "./korakpokorak";
import { MojBrojGame } from "./mojbroj";
import { finishMatch, recordGamePoints, type MatchResult, type Outcome } from "./persist";
import type { GameType } from "../model/stats";
import type { Game, GameContext, GameMessage, Scores, Slot } from "./types";

const KIND_TO_GAMETYPE: Record<string, GameType> = {
  mojbroj: "moj_broj",
  korakpokorak: "korak_po_korak",
  skocko: "skocko",
  koznazna: "ko_zna_zna",
  spojnice: "spojnice",
  asocijacije: "asocijacije",
};

export interface MatchPlayer {
  userId: number;
  username: string;
}

// Same order as the menu: Moj broj -> Korak po korak -> Skočko -> Ko zna zna
// -> Spojnice -> Asocijacije.
const GAME_SEQUENCE = [
  "mojbroj",
  "korakpokorak",
  "skocko",
  "koznazna",
  "spojnice",
  "asocijacije",
] as const;

type GameKind = (typeof GAME_SEQUENCE)[number];

function createGame(kind: GameKind, ctx: GameContext): Game {
  switch (kind) {
    case "koznazna": return new KoZnaZnaGame(ctx);
    case "spojnice": return new SpojniceGame(ctx);
    case "asocijacije": return new AsocijacijeGame(ctx);
    case "skocko": return new SkockoGame(ctx);
    case "korakpokorak": return new KorakPoKorakGame(ctx);
    case "mojbroj": return new MojBrojGame(ctx);
  }
}

export class Match {
  readonly id: string;
  private readonly players: [MatchPlayer, MatchPlayer];
  private readonly total: Scores = [0, 0];
  private gameIndex = 0;
  private current: Game | null = null;
  private over = false;

  constructor(
    id: string,
    p0: MatchPlayer,
    p1: MatchPlayer,
    private readonly onFinished: (match: Match) => void,
  ) {
    this.id = id;
    this.players = [p0, p1];
  }

  get userIds(): [number, number] {
    return [this.players[0].userId, this.players[1].userId];
  }

  slotOf(userId: number): Slot | null {
    if (this.players[0].userId === userId) return 0;
    if (this.players[1].userId === userId) return 1;
    return null;
  }

  start(): void {
    for (const slot of [0, 1] as Slot[]) {
      pushToUser(this.players[slot].userId, {
        type: "match_found",
        matchId: this.id,
        you: slot,
        opponent: this.players[slot === 0 ? 1 : 0].username,
        games: GAME_SEQUENCE,
      });
    }
    this.runCurrentGame();
  }

  private runCurrentGame(): void {
    const ctx: GameContext = {
      userIds: this.userIds,
      usernames: [this.players[0].username, this.players[1].username],
      sendTo: (slot, payload) => pushToUser(this.players[slot].userId, payload),
      broadcast: (payload) => {
        pushToUser(this.players[0].userId, payload);
        pushToUser(this.players[1].userId, payload);
      },
      complete: (scores) => this.onGameComplete(scores),
    };

    const kind = GAME_SEQUENCE[this.gameIndex]!;
    console.log(`[match ${this.id.slice(0, 6)}] start game ${this.gameIndex + 1}/${GAME_SEQUENCE.length}: ${kind}`);
    this.current = createGame(kind, ctx);
    try {
      this.current.start();
    } catch (err) {
      console.error(`[match] game ${kind} start error:`, err);
    }
  }

  private onGameComplete(scores: Scores): void {
    const kind = GAME_SEQUENCE[this.gameIndex]!;
    console.log(`[match ${this.id.slice(0, 6)}] game ${kind} complete: ${JSON.stringify(scores)}`);
    const gameType = KIND_TO_GAMETYPE[kind];
    if (gameType) {
      try {
        recordGamePoints(this.players[0].userId, gameType, scores[0]);
        recordGamePoints(this.players[1].userId, gameType, scores[1]);
      } catch (err) {
        console.error("Failed to record game points:", err);
      }
    }
    this.total[0] += scores[0];
    this.total[1] += scores[1];

    [0, 1].forEach((slot) =>
      pushToUser(this.players[slot as Slot].userId, {
        type: "game_complete",
        game: GAME_SEQUENCE[this.gameIndex],
        gameScores: scores,
        totalScores: this.total,
      }),
    );

    this.gameIndex++;
    if (this.gameIndex >= GAME_SEQUENCE.length) {
      this.finish();
    } else {
      // small delay so the client can show the inter-game scoreboard
      setTimeout(() => {
        if (!this.over) this.runCurrentGame();
      }, 2500);
    }
  }

  handleMessage(userId: number, msg: GameMessage): void {
    if (this.over) return;
    const slot = this.slotOf(userId);
    if (slot === null) return;
    this.current?.handleMessage(slot, msg);
  }

  // A player abandoned the match (disconnect or explicit leave): they lose,
  // the opponent wins with the points accumulated so far.
  handleLeave(userId: number): void {
    if (this.over) return;
    const slot = this.slotOf(userId);
    if (slot === null) return;
    const winnerSlot: Slot = slot === 0 ? 1 : 0;

    this.current?.dispose();
    this.current = null;

    pushToUser(this.players[winnerSlot].userId, {
      type: "opponent_left",
    });

    this.concludeMatch(winnerSlot, "abandon");
  }

  private finish(): void {
    let winnerSlot: Slot | null;
    if (this.total[0] > this.total[1]) winnerSlot = 0;
    else if (this.total[1] > this.total[0]) winnerSlot = 1;
    else winnerSlot = null;
    this.concludeMatch(winnerSlot, "normal");
  }

  private concludeMatch(winnerSlot: Slot | null, reason: "normal" | "abandon"): void {
    if (this.over) return;
    this.over = true;

    const outcomeFor = (slot: Slot): Outcome => {
      if (winnerSlot === null) return "tie";
      return slot === winnerSlot ? "win" : "loss";
    };

    const results: [MatchResult, MatchResult] = [
      { userId: this.players[0].userId, points: this.total[0], outcome: outcomeFor(0) },
      { userId: this.players[1].userId, points: this.total[1], outcome: outcomeFor(1) },
    ];

    let starsDelta: Record<number, number> = {};
    try {
      starsDelta = finishMatch(results[0], results[1]);
    } catch (err) {
      console.error("Failed to persist match result:", err);
    }

    for (const slot of [0, 1] as Slot[]) {
      const player = this.players[slot];
      const won = winnerSlot === slot;
      const delta = starsDelta[player.userId] ?? 0;
      const title = winnerSlot === null ? "Nerešeno" : won ? "Pobeda!" : "Poraz";
      const sign = delta >= 0 ? "+" : "";
      try {
        createNotification(
          player.userId,
          "NAGRADE",
          title,
          `Partija završena ${this.total[0]}:${this.total[1]}. Zvezde: ${sign}${delta}.`,
        );
      } catch (err) {
        console.error("Failed to create match notification:", err);
      }
    }

    for (const slot of [0, 1] as Slot[]) {
      const player = this.players[slot];
      pushToUser(player.userId, {
        type: "match_over",
        reason,
        totalScores: this.total,
        youWon: winnerSlot === slot,
        tie: winnerSlot === null,
        winner: winnerSlot === null ? null : this.players[winnerSlot].username,
        starsDelta: starsDelta[player.userId] ?? 0,
      });
    }

    this.onFinished(this);
  }

  dispose(): void {
    this.over = true;
    this.current?.dispose();
    this.current = null;
  }
}
