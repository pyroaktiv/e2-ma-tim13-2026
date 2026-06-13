// Skočko — real-time, server-authoritative.
// 2 rounds. The round's lead player has up to 6 attempts (30s) to find a secret
// combination of 4 symbols (out of 6). Guess in attempts 1-2 = 20 pts, 3-4 = 15,
// 5-6 = 10. If the lead fails, the opponent gets one 10s attempt for 10 pts.

import { SKOCKO_LENGTH, SKOCKO_SYMBOLS } from "./data";
import { recordSkocko } from "./persist";
import { skockoFeedback, skockoCellHints } from "./util";
import { other, type Game, type GameContext, type GameMessage, type Scores, type Slot } from "./types";

const ROUND_COUNT = 2;
const MAX_ATTEMPTS = 6;
const LEAD_MS = 30000;
const STEAL_MS = 10000;
const ROUND_PAUSE_MS = 1800;

type Phase = "lead" | "steal";

function randomSecret(): number[] {
  return Array.from({ length: SKOCKO_LENGTH }, () => Math.floor(Math.random() * SKOCKO_SYMBOLS));
}

export class SkockoGame implements Game {
  private readonly scores: Scores = [0, 0];
  private readonly correctAtAttempt: [number[], number[]] = [
    [0, 0, 0, 0, 0, 0],
    [0, 0, 0, 0, 0, 0],
  ];
  private readonly failed: Scores = [0, 0];

  private roundIndex = 0;
  private leadPlayer: Slot = 0;
  private activePlayer: Slot = 0;
  private phase: Phase = "lead";
  private attempts = 0;
  private secret: number[] = [];
  private roundOver = false;
  private timer: ReturnType<typeof setTimeout> | null = null;
  private disposed = false;

  constructor(private readonly ctx: GameContext) {}

  start(): void {
    this.ctx.broadcast({ type: "game_intro", game: "skocko", total: 6 });
    this.startRound();
  }

  private startRound(): void {
    if (this.disposed) return;
    this.secret = randomSecret();
    this.leadPlayer = (this.roundIndex === 0 ? 0 : 1) as Slot;
    this.activePlayer = this.leadPlayer;
    this.phase = "lead";
    this.attempts = 0;
    this.roundOver = false;
    this.broadcastState(LEAD_MS);
    this.startTimer(LEAD_MS);
  }

  private broadcastState(timeMs: number): void {
    this.ctx.broadcast({
      type: "skocko_round",
      round: this.roundIndex + 1,
      totalRounds: ROUND_COUNT,
      symbols: SKOCKO_SYMBOLS,
      length: SKOCKO_LENGTH,
      activePlayer: this.activePlayer,
      phase: this.phase,
      maxAttempts: this.phase === "lead" ? MAX_ATTEMPTS : 1,
      timeMs,
      scores: this.scores,
    });
  }

  private startTimer(ms: number): void {
    if (this.timer) clearTimeout(this.timer);
    this.timer = setTimeout(() => this.onTimeout(), ms);
  }

  handleMessage(slot: Slot, msg: GameMessage): void {
    if (this.disposed || this.roundOver || msg.type !== "skocko_guess") return;
    if (slot !== this.activePlayer) return;
    const guess = msg.guess;
    if (!Array.isArray(guess) || guess.length !== SKOCKO_LENGTH) return;
    if (!guess.every((g) => Number.isInteger(g) && g >= 0 && g < SKOCKO_SYMBOLS)) return;

    this.attempts++;
    const fb = skockoFeedback(this.secret, guess as number[]);
    const hints = skockoCellHints(this.secret, guess as number[]);
    const solved = fb.exact === SKOCKO_LENGTH;

    if (solved) {
      const pts = this.attempts <= 2 ? 20 : this.attempts <= 4 ? 15 : 10;
      const award = this.phase === "steal" ? 10 : pts;
      this.scores[this.activePlayer] += award;
      const idx = Math.min(this.attempts - 1, MAX_ATTEMPTS - 1);
      this.correctAtAttempt[this.activePlayer][idx]!++;
    }

    this.ctx.broadcast({
      type: "skocko_feedback",
      by: this.activePlayer,
      guess,
      hints,
      exact: fb.exact,
      color: fb.color,
      attempt: this.attempts,
      solved,
      scores: this.scores,
    });

    if (solved) {
      this.endRound();
    } else if (this.phase === "lead" && this.attempts >= MAX_ATTEMPTS) {
      this.startSteal();
    } else if (this.phase === "steal") {
      this.endRound();
    }
  }

  private startSteal(): void {
    this.failed[this.leadPlayer]++;
    this.phase = "steal";
    this.activePlayer = other(this.leadPlayer);
    this.attempts = 0;
    this.broadcastState(STEAL_MS);
    this.startTimer(STEAL_MS);
  }

  private onTimeout(): void {
    if (this.disposed) return;
    this.timer = null;
    if (this.phase === "lead") this.startSteal();
    else this.endRound();
  }

  private endRound(): void {
    if (this.disposed || this.roundOver) return;
    this.roundOver = true;
    if (this.timer) clearTimeout(this.timer);
    this.timer = null;
    this.ctx.broadcast({ type: "skocko_round_end", secret: this.secret, scores: this.scores });
    this.timer = setTimeout(() => {
      this.roundIndex++;
      if (this.roundIndex >= ROUND_COUNT) this.finish();
      else this.startRound();
    }, ROUND_PAUSE_MS);
  }

  private finish(): void {
    if (this.disposed) return;
    recordSkocko(this.ctx.userIds[0], this.correctAtAttempt[0], this.failed[0]);
    recordSkocko(this.ctx.userIds[1], this.correctAtAttempt[1], this.failed[1]);
    this.ctx.complete([this.scores[0], this.scores[1]]);
  }

  dispose(): void {
    this.disposed = true;
    if (this.timer) clearTimeout(this.timer);
    this.timer = null;
  }
}
