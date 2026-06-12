// Moj broj — real-time, server-authoritative.
// 2 rounds, max 10 pts/round. Both players get the same target and 6 numbers
// (4 single-digit, one of {10,15,20}, one of {25,50,75,100}) and have 60s to
// build an expression. Exact target = 10 pts; otherwise the closer result = 5;
// on a tie the round's starter takes the 5. No input = 0.

import { shuffle } from "./data";
import { recordMojBroj } from "./persist";
import { evalMojBroj } from "./util";
import type { Game, GameContext, GameMessage, Scores, Slot } from "./types";

const ROUND_COUNT = 2;
const ROUND_MS = 60000;
const ROUND_PAUSE_MS = 2000;
const EPS = 1e-9;

function pick<T>(arr: readonly T[]): T {
  return arr[Math.floor(Math.random() * arr.length)]!;
}

export class MojBrojGame implements Game {
  private readonly scores: Scores = [0, 0];
  private readonly attempts: Scores = [0, 0];
  private readonly exactHits: Scores = [0, 0];

  private roundIndex = 0;
  private target = 0;
  private numbers: number[] = [];
  private results: (number | null)[] = [null, null];
  private submitted: boolean[] = [false, false];
  private timer: ReturnType<typeof setTimeout> | null = null;
  private disposed = false;

  constructor(private readonly ctx: GameContext) {}

  start(): void {
    this.ctx.broadcast({ type: "game_intro", game: "mojbroj", total: 6 });
    this.startRound();
  }

  private startRound(): void {
    if (this.disposed) return;
    const singles = Array.from({ length: 4 }, () => 1 + Math.floor(Math.random() * 9));
    this.numbers = shuffle([...singles, pick([10, 15, 20]), pick([25, 50, 75, 100])]);
    this.target = 101 + Math.floor(Math.random() * 899);
    this.results = [null, null];
    this.submitted = [false, false];

    this.ctx.broadcast({
      type: "mojbroj_round",
      round: this.roundIndex + 1,
      totalRounds: ROUND_COUNT,
      target: this.target,
      numbers: this.numbers,
      timeMs: ROUND_MS,
      scores: this.scores,
    });
    this.timer = setTimeout(() => this.resolve(), ROUND_MS);
  }

  handleMessage(slot: Slot, msg: GameMessage): void {
    if (this.disposed || msg.type !== "mojbroj_submit") return;
    if (this.submitted[slot]) return;
    const expr = typeof msg.expr === "string" ? msg.expr : "";
    this.submitted[slot] = true;
    this.results[slot] = expr.trim() === "" ? null : evalMojBroj(expr, this.numbers);

    this.ctx.sendTo(slot === 0 ? 1 : 0, { type: "mojbroj_opponent_submitted" });

    if (this.submitted[0] && this.submitted[1]) {
      if (this.timer) clearTimeout(this.timer);
      this.resolve();
    }
  }

  private resolve(): void {
    if (this.disposed) return;
    this.timer = null;
    const roundStarter: Slot = (this.roundIndex === 0 ? 0 : 1) as Slot;
    const ra = this.results[0] ?? null;
    const rb = this.results[1] ?? null;
    const exactA = ra !== null && Math.abs(ra - this.target) < EPS;
    const exactB = rb !== null && Math.abs(rb - this.target) < EPS;
    const deltas: Scores = [0, 0];

    if (exactA) deltas[0] = 10;
    if (exactB) deltas[1] = 10;

    if (!exactA && !exactB) {
      const validA = ra !== null;
      const validB = rb !== null;
      if (validA && validB) {
        const dA = Math.abs(ra! - this.target);
        const dB = Math.abs(rb! - this.target);
        if (dA < dB) deltas[0] = 5;
        else if (dB < dA) deltas[1] = 5;
        else deltas[roundStarter] = 5;
      } else if (validA) deltas[0] = 5;
      else if (validB) deltas[1] = 5;
    }

    for (const slot of [0, 1] as Slot[]) {
      if (this.results[slot] !== null) this.attempts[slot]++;
      if ((slot === 0 ? exactA : exactB)) this.exactHits[slot]++;
      this.scores[slot] += deltas[slot];
    }

    for (const slot of [0, 1] as Slot[]) {
      this.ctx.sendTo(slot, {
        type: "mojbroj_result",
        target: this.target,
        yourResult: this.results[slot],
        opponentResult: this.results[slot === 0 ? 1 : 0],
        deltas,
        scores: this.scores,
      });
    }

    this.timer = setTimeout(() => {
      this.roundIndex++;
      if (this.roundIndex >= ROUND_COUNT) this.finish();
      else this.startRound();
    }, ROUND_PAUSE_MS);
  }

  private finish(): void {
    if (this.disposed) return;
    recordMojBroj(this.ctx.userIds[0], this.attempts[0], this.exactHits[0]);
    recordMojBroj(this.ctx.userIds[1], this.attempts[1], this.exactHits[1]);
    this.ctx.complete([this.scores[0], this.scores[1]]);
  }

  dispose(): void {
    this.disposed = true;
    if (this.timer) clearTimeout(this.timer);
    this.timer = null;
  }
}
