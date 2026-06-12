// Spojnice — real-time, server-authoritative.
// 2 rounds, 30s each. One player leads a round and gets up to 5 attempts to
// connect the 5 left prompts to the right ones. If unconnected pairs remain,
// the other player gets a chance ("popravka") to connect the rest. Each
// correct connection is worth 2 points (max 10 per round). (Spec: igra 2.)

import { SPOJNICE_POOL, shuffle, type SpojniceRound } from "./data";
import { recordSpojnice } from "./persist";
import { other, type Game, type GameContext, type GameMessage, type Scores, type Slot } from "./types";

const ROUND_COUNT = 2;
const PHASE_MS = 30000;
const LEAD_ATTEMPTS = 5;
const ROUND_PAUSE_MS = 1500;

type Phase = "lead" | "repair";

export class SpojniceGame implements Game {
  private readonly rounds: SpojniceRound[];
  private readonly scores: Scores = [0, 0];
  private readonly successful: Scores = [0, 0];
  private readonly totalAttempts: Scores = [0, 0];

  private roundIndex = 0;
  private leadPlayer: Slot = 0;
  private activePlayer: Slot = 0;
  private phase: Phase = "lead";
  private attempts = 0;
  private repairAllowed = 0;
  private connectedCount = 0;
  private connectedLeft: boolean[] = [];
  private matchedRight: number[] = [];
  private roundOver = false;
  private timer: ReturnType<typeof setTimeout> | null = null;
  private disposed = false;

  constructor(private readonly ctx: GameContext) {
    this.rounds = shuffle(SPOJNICE_POOL).slice(0, ROUND_COUNT);
  }

  start(): void {
    this.ctx.broadcast({ type: "game_intro", game: "spojnice", total: 6 });
    this.startRound();
  }

  private startRound(): void {
    if (this.disposed) return;
    const round = this.rounds[this.roundIndex]!;
    this.leadPlayer = (this.roundIndex === 0 ? 0 : 1) as Slot;
    this.activePlayer = this.leadPlayer;
    this.phase = "lead";
    this.attempts = 0;
    this.repairAllowed = 0;
    this.connectedCount = 0;
    this.connectedLeft = new Array(round.left.length).fill(false);
    this.matchedRight = new Array(round.left.length).fill(-1);
    this.roundOver = false;

    this.ctx.broadcast({
      type: "spj_round",
      round: this.roundIndex + 1,
      totalRounds: this.rounds.length,
      criterion: round.criterion,
      left: round.left,
      right: round.right,
      activePlayer: this.activePlayer,
      phase: this.phase,
      timeMs: PHASE_MS,
      scores: this.scores,
    });
    this.startTimer();
  }

  private startTimer(): void {
    if (this.timer) clearTimeout(this.timer);
    this.timer = setTimeout(() => this.onTimeout(), PHASE_MS);
  }

  handleMessage(slot: Slot, msg: GameMessage): void {
    if (this.disposed || this.roundOver || msg.type !== "spojnice_connect") return;
    if (slot !== this.activePlayer) return; // not your turn

    const round = this.rounds[this.roundIndex]!;
    const left = Number(msg.left);
    const right = Number(msg.right);
    if (
      !Number.isInteger(left) || !Number.isInteger(right) ||
      left < 0 || left >= round.left.length ||
      right < 0 || right >= round.right.length
    ) return;
    if (this.connectedLeft[left]) return; // already connected
    if (this.matchedRight.includes(right)) return; // right already used

    this.attempts++;
    this.totalAttempts[this.activePlayer]++;
    const correct = round.solution[left] === right;

    if (correct) {
      this.connectedLeft[left] = true;
      this.matchedRight[left] = right;
      this.connectedCount++;
      this.scores[this.activePlayer] += 2;
      this.successful[this.activePlayer]++;
    }

    this.ctx.broadcast({
      type: "spj_connect",
      left,
      right,
      correct,
      by: this.activePlayer,
      scores: this.scores,
    });

    this.advance();
  }

  private advance(): void {
    if (this.connectedCount >= this.rounds[this.roundIndex]!.left.length) {
      this.endRound();
      return;
    }
    if (this.phase === "lead") {
      if (this.attempts >= LEAD_ATTEMPTS) this.switchToRepair();
    } else {
      if (this.attempts >= this.repairAllowed) this.endRound();
    }
  }

  private switchToRepair(): void {
    const total = this.rounds[this.roundIndex]!.left.length;
    this.repairAllowed = total - this.connectedCount;
    if (this.repairAllowed <= 0) {
      this.endRound();
      return;
    }
    this.phase = "repair";
    this.activePlayer = other(this.leadPlayer);
    this.attempts = 0;
    if (this.timer) clearTimeout(this.timer);
    this.ctx.broadcast({
      type: "spj_turn",
      activePlayer: this.activePlayer,
      phase: this.phase,
      timeMs: PHASE_MS,
      scores: this.scores,
    });
    this.startTimer();
  }

  private onTimeout(): void {
    if (this.disposed) return;
    this.timer = null;
    if (this.phase === "lead") this.switchToRepair();
    else this.endRound();
  }

  private endRound(): void {
    if (this.disposed || this.roundOver) return;
    this.roundOver = true;
    if (this.timer) clearTimeout(this.timer);
    this.timer = null;

    const round = this.rounds[this.roundIndex]!;
    // reveal the full correct mapping for unconnected pairs
    this.ctx.broadcast({
      type: "spj_round_end",
      round: this.roundIndex + 1,
      solution: round.solution,
      scores: this.scores,
    });

    this.timer = setTimeout(() => {
      this.roundIndex++;
      if (this.roundIndex >= this.rounds.length) this.finish();
      else this.startRound();
    }, ROUND_PAUSE_MS);
  }

  private finish(): void {
    if (this.disposed) return;
    recordSpojnice(this.ctx.userIds[0], this.successful[0], this.totalAttempts[0]);
    recordSpojnice(this.ctx.userIds[1], this.successful[1], this.totalAttempts[1]);
    this.ctx.complete([this.scores[0], this.scores[1]]);
  }

  dispose(): void {
    this.disposed = true;
    if (this.timer) clearTimeout(this.timer);
    this.timer = null;
  }
}
