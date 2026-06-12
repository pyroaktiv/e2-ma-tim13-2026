// Asocijacije — real-time, server-authoritative.
// 2 rounds, max 30 pts/round. 4 columns of 4 hidden fields, each column has a
// solution; the column solutions point to a final solution. Players alternate:
// on your turn you open one field, then may guess column/final solutions. A
// correct column lets you keep guessing; a wrong guess (or pass) ends your turn.
// Column = 2 + 1 per unopened field. Final = 7 + 6 per untouched column
// + (2 + unopened) for partially opened (unsolved) columns.

import { ASOCIJACIJE_POOL, shuffle, type AsocijacijeRound } from "./data";
import { recordAsocijacije } from "./persist";
import { matchesAnswer } from "./util";
import { other, type Game, type GameContext, type GameMessage, type Scores, type Slot } from "./types";

const ROUND_COUNT = 2;
const NUM_COLS = 4;
const NUM_FIELDS = 4;
const ROUND_MS = 120000;
const ROUND_PAUSE_MS = 2500;

export class AsocijacijeGame implements Game {
  private readonly rounds: AsocijacijeRound[];
  private readonly scores: Scores = [0, 0];
  private readonly solved: Scores = [0, 0];
  private readonly unsolved: Scores = [0, 0];

  private roundIndex = 0;
  private leadPlayer: Slot = 0;
  private activePlayer: Slot = 0;
  private turnOpened = false;
  private opened: boolean[][] = [];
  private openedCount: number[] = [];
  private columnSolved: boolean[] = [];
  private finalSolved = false;
  private roundOver = false;
  private timer: ReturnType<typeof setTimeout> | null = null;
  private disposed = false;

  constructor(private readonly ctx: GameContext) {
    this.rounds = shuffle(ASOCIJACIJE_POOL).slice(0, ROUND_COUNT);
  }

  start(): void {
    this.ctx.broadcast({ type: "game_intro", game: "asocijacije", total: 6 });
    this.startRound();
  }

  private get round(): AsocijacijeRound { return this.rounds[this.roundIndex]!; }

  private startRound(): void {
    if (this.disposed) return;
    this.leadPlayer = (this.roundIndex === 0 ? 0 : 1) as Slot;
    this.activePlayer = this.leadPlayer;
    this.turnOpened = false;
    this.opened = Array.from({ length: NUM_COLS }, () => new Array(NUM_FIELDS).fill(false));
    this.openedCount = new Array(NUM_COLS).fill(0);
    this.columnSolved = new Array(NUM_COLS).fill(false);
    this.finalSolved = false;
    this.roundOver = false;

    this.ctx.broadcast({
      type: "aso_round",
      round: this.roundIndex + 1,
      totalRounds: ROUND_COUNT,
      cols: NUM_COLS,
      fields: NUM_FIELDS,
      activePlayer: this.activePlayer,
      timeMs: ROUND_MS,
      scores: this.scores,
    });
    this.timer = setTimeout(() => this.endRound(), ROUND_MS);
  }

  private anyClosed(): boolean {
    return this.openedCount.some((c) => c < NUM_FIELDS);
  }

  handleMessage(slot: Slot, msg: GameMessage): void {
    if (this.disposed || this.roundOver || slot !== this.activePlayer) return;
    switch (msg.type) {
      case "aso_open": return this.handleOpen(slot, msg);
      case "aso_guess": return this.handleGuess(slot, msg);
      case "aso_pass": return this.endTurn();
    }
  }

  private handleOpen(slot: Slot, msg: GameMessage): void {
    if (this.turnOpened) return;
    const col = Number(msg.col);
    const field = Number(msg.field);
    if (!this.validCell(col, field) || this.opened[col]![field]) return;
    this.opened[col]![field] = true;
    this.openedCount[col]!++;
    this.turnOpened = true;
    this.ctx.broadcast({
      type: "aso_field",
      col,
      field,
      text: this.round.columns[col]!.fields[field],
      by: slot,
      scores: this.scores,
    });
  }

  private handleGuess(slot: Slot, msg: GameMessage): void {
    // must open a field first, unless the whole board is already open
    if (!this.turnOpened && this.anyClosed()) return;
    const target = msg.target;
    const text = typeof msg.text === "string" ? msg.text : "";

    if (target === "column") {
      const col = Number(msg.col);
      if (col < 0 || col >= NUM_COLS || this.columnSolved[col]) return;
      const column = this.round.columns[col]!;
      if (matchesAnswer(text, column.accepted)) {
        const pts = 2 + (NUM_FIELDS - this.openedCount[col]!);
        this.scores[slot] += pts;
        this.columnSolved[col] = true;
        this.ctx.broadcast({
          type: "aso_solved",
          target: "column",
          col,
          text: column.solution,
          points: pts,
          by: slot,
          scores: this.scores,
        });
        // correct column -> keep guessing (turn continues)
      } else {
        this.ctx.broadcast({ type: "aso_wrong", target: "column", col, by: slot });
        this.endTurn();
      }
      return;
    }

    if (target === "final") {
      if (matchesAnswer(text, this.round.finalAccepted)) {
        const bonus = this.finalBonus();
        this.scores[slot] += bonus;
        this.finalSolved = true;
        this.solved[slot]++;
        this.ctx.broadcast({
          type: "aso_solved",
          target: "final",
          text: this.round.finalSolution,
          points: bonus,
          by: slot,
          scores: this.scores,
        });
        this.endRound();
      } else {
        this.ctx.broadcast({ type: "aso_wrong", target: "final", by: slot });
        this.endTurn();
      }
    }
  }

  private finalBonus(): number {
    let bonus = 7;
    for (let c = 0; c < NUM_COLS; c++) {
      if (this.columnSolved[c]) continue; // already scored
      bonus += this.openedCount[c] === 0 ? 6 : 2 + (NUM_FIELDS - this.openedCount[c]!);
    }
    return bonus;
  }

  private endTurn(): void {
    if (this.disposed || this.finalSolved) return;
    this.activePlayer = other(this.activePlayer);
    this.turnOpened = false;
    this.ctx.broadcast({ type: "aso_turn", activePlayer: this.activePlayer, scores: this.scores });
  }

  private endRound(): void {
    if (this.disposed || this.roundOver) return;
    this.roundOver = true;
    if (this.timer) clearTimeout(this.timer);
    this.timer = null;
    if (!this.finalSolved) {
      this.unsolved[0]++;
      this.unsolved[1]++;
    }
    this.ctx.broadcast({
      type: "aso_round_end",
      finalSolution: this.round.finalSolution,
      columnSolutions: this.round.columns.map((c) => c.solution),
      scores: this.scores,
    });
    this.timer = setTimeout(() => {
      this.roundIndex++;
      if (this.roundIndex >= ROUND_COUNT) this.finish();
      else this.startRound();
    }, ROUND_PAUSE_MS);
  }

  private validCell(col: number, field: number): boolean {
    return Number.isInteger(col) && Number.isInteger(field) &&
      col >= 0 && col < NUM_COLS && field >= 0 && field < NUM_FIELDS;
  }

  private finish(): void {
    if (this.disposed) return;
    recordAsocijacije(this.ctx.userIds[0], this.solved[0], this.unsolved[0]);
    recordAsocijacije(this.ctx.userIds[1], this.solved[1], this.unsolved[1]);
    this.ctx.complete([this.scores[0], this.scores[1]]);
  }

  dispose(): void {
    this.disposed = true;
    if (this.timer) clearTimeout(this.timer);
    this.timer = null;
  }
}
