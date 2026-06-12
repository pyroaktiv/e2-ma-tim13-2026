// Korak po korak — real-time, server-authoritative.
// 2 rounds, max 20 pts/round. The round's lead player must guess a term. A new
// clue (of 7, hardest first) is revealed every 10s. Guessing at step 1 = 20 pts,
// losing 2 pts per advanced step. If the lead fails, the opponent gets 10s to
// guess for 5 pts.

import { KORAK_POOL, shuffle, type KorakPoKorakRound } from "./data";
import { recordKorakPoKorak } from "./persist";
import { matchesAnswer } from "./util";
import { other, type Game, type GameContext, type GameMessage, type Scores, type Slot } from "./types";

const ROUND_COUNT = 2;
const CLUE_COUNT = 7;
const CLUE_MS = 10000;
const STEAL_MS = 10000;
const ROUND_PAUSE_MS = 1800;

type Phase = "lead" | "steal";

export class KorakPoKorakGame implements Game {
  private readonly rounds: KorakPoKorakRound[];
  private readonly scores: Scores = [0, 0];
  private readonly guessedAtStep: [number[], number[]] = [
    [0, 0, 0, 0, 0, 0, 0],
    [0, 0, 0, 0, 0, 0, 0],
  ];

  private roundIndex = 0;
  private leadPlayer: Slot = 0;
  private activePlayer: Slot = 0;
  private phase: Phase = "lead";
  private revealed = 0;
  private roundOver = false;
  private timer: ReturnType<typeof setTimeout> | null = null;
  private disposed = false;

  constructor(private readonly ctx: GameContext) {
    this.rounds = shuffle(KORAK_POOL).slice(0, ROUND_COUNT);
  }

  start(): void {
    this.ctx.broadcast({ type: "game_intro", game: "korakpokorak", total: 6 });
    this.startRound();
  }

  private get round(): KorakPoKorakRound { return this.rounds[this.roundIndex]!; }

  private startRound(): void {
    if (this.disposed) return;
    this.leadPlayer = (this.roundIndex === 0 ? 0 : 1) as Slot;
    this.activePlayer = this.leadPlayer;
    this.phase = "lead";
    this.revealed = 1;
    this.roundOver = false;
    this.ctx.broadcast({
      type: "korak_round",
      round: this.roundIndex + 1,
      totalRounds: ROUND_COUNT,
      activePlayer: this.activePlayer,
      totalClues: CLUE_COUNT,
      timeMs: CLUE_COUNT * CLUE_MS,
      scores: this.scores,
    });
    this.sendClue();
    this.timer = setTimeout(() => this.onTick(), CLUE_MS);
  }

  private sendClue(): void {
    this.ctx.broadcast({
      type: "korak_clue",
      index: this.revealed - 1,
      total: CLUE_COUNT,
      text: this.round.clues[this.revealed - 1] ?? "",
      step: this.revealed,
      possiblePoints: Math.max(0, 20 - 2 * (this.revealed - 1)),
    });
  }

  private onTick(): void {
    if (this.disposed) return;
    this.timer = null;
    if (this.revealed < CLUE_COUNT) {
      this.revealed++;
      this.sendClue();
      this.timer = setTimeout(() => this.onTick(), CLUE_MS);
    } else {
      this.startSteal();
    }
  }

  handleMessage(slot: Slot, msg: GameMessage): void {
    if (this.disposed || this.roundOver || msg.type !== "korak_guess") return;
    if (slot !== this.activePlayer) return;
    const text = typeof msg.text === "string" ? msg.text : "";
    const correct = matchesAnswer(text, this.round.accepted);

    if (!correct) {
      this.ctx.broadcast({ type: "korak_wrong", by: slot });
      return;
    }

    if (this.phase === "lead") {
      const pts = Math.max(0, 20 - 2 * (this.revealed - 1));
      this.scores[slot] += pts;
      this.guessedAtStep[slot][this.revealed - 1]!++;
    } else {
      this.scores[slot] += 5;
    }
    this.ctx.broadcast({
      type: "korak_correct",
      by: slot,
      answer: this.round.answer,
      scores: this.scores,
    });
    this.endRound();
  }

  private startSteal(): void {
    if (this.timer) clearTimeout(this.timer);
    this.phase = "steal";
    this.activePlayer = other(this.leadPlayer);
    this.ctx.broadcast({
      type: "korak_steal",
      activePlayer: this.activePlayer,
      timeMs: STEAL_MS,
      scores: this.scores,
    });
    this.timer = setTimeout(() => this.endRound(), STEAL_MS);
  }

  private endRound(): void {
    if (this.disposed || this.roundOver) return;
    this.roundOver = true;
    if (this.timer) clearTimeout(this.timer);
    this.timer = null;
    this.ctx.broadcast({ type: "korak_round_end", answer: this.round.answer, scores: this.scores });
    this.timer = setTimeout(() => {
      this.roundIndex++;
      if (this.roundIndex >= ROUND_COUNT) this.finish();
      else this.startRound();
    }, ROUND_PAUSE_MS);
  }

  private finish(): void {
    if (this.disposed) return;
    recordKorakPoKorak(this.ctx.userIds[0], this.guessedAtStep[0]);
    recordKorakPoKorak(this.ctx.userIds[1], this.guessedAtStep[1]);
    this.ctx.complete([this.scores[0], this.scores[1]]);
  }

  dispose(): void {
    this.disposed = true;
    if (this.timer) clearTimeout(this.timer);
    this.timer = null;
  }
}
