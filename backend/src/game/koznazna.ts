// Ko zna zna — real-time, server-authoritative.
// 5 questions, 5s each. Correct +10, wrong -5. If both answer correctly the
// faster player takes the 10 points (the slower correct answer scores 0).
// If nobody answers, the score is unchanged. (Spec: igra 1, "Ko zna zna".)

import { KO_ZNA_ZNA_POOL, shuffle, type KoZnaZnaQuestion } from "./data";
import { recordKoZnaZna } from "./persist";
import type { Game, GameContext, GameMessage, Scores, Slot } from "./types";

const QUESTION_COUNT = 5;
const ANSWER_MS = 5000;
const RESULT_PAUSE_MS = 1800;

export class KoZnaZnaGame implements Game {
  private readonly questions: KoZnaZnaQuestion[];
  private readonly scores: Scores = [0, 0];
  private readonly correctCount: Scores = [0, 0];
  private readonly missedCount: Scores = [0, 0];

  private index = 0;
  private answers: (number | null)[] = [null, null];
  private answerTimes: number[] = [0, 0];
  private accepting = false;
  private timer: ReturnType<typeof setTimeout> | null = null;
  private disposed = false;

  constructor(private readonly ctx: GameContext) {
    this.questions = shuffle(KO_ZNA_ZNA_POOL).slice(0, QUESTION_COUNT);
  }

  start(): void {
    this.ctx.broadcast({ type: "game_intro", game: "koznazna", total: 6 });
    this.askQuestion();
  }

  private askQuestion(): void {
    if (this.disposed) return;
    this.answers = [null, null];
    this.answerTimes = [0, 0];
    this.accepting = true;
    const q = this.questions[this.index]!;
    this.ctx.broadcast({
      type: "kzz_question",
      index: this.index,
      total: this.questions.length,
      text: q.text,
      answers: q.answers,
      timeMs: ANSWER_MS,
      scores: this.scores,
    });
    this.timer = setTimeout(() => this.resolveQuestion(), ANSWER_MS);
  }

  handleMessage(slot: Slot, msg: GameMessage): void {
    if (this.disposed || !this.accepting || msg.type !== "kzz_answer") return;
    if (this.answers[slot] !== null) return; // already answered
    const answer = Number(msg.answer);
    if (!Number.isInteger(answer) || answer < 0 || answer > 3) return;

    this.answers[slot] = answer;
    this.answerTimes[slot] = Date.now();

    // live feedback to the opponent
    this.ctx.sendTo(slot === 0 ? 1 : 0, { type: "kzz_opponent_answered" });

    if (this.answers[0] !== null && this.answers[1] !== null) {
      if (this.timer) clearTimeout(this.timer);
      this.resolveQuestion();
    }
  }

  private resolveQuestion(): void {
    if (this.disposed || !this.accepting) return;
    this.accepting = false;
    this.timer = null;
    const q = this.questions[this.index]!;
    const deltas: Scores = [0, 0];

    const correctSlots: Slot[] = [];
    for (const slot of [0, 1] as Slot[]) {
      const ans = this.answers[slot];
      if (ans === null) continue;
      if (ans === q.correct) {
        correctSlots.push(slot);
        this.correctCount[slot]++;
      } else {
        deltas[slot] = -5;
        this.missedCount[slot]++;
      }
    }

    if (correctSlots.length === 1) {
      deltas[correctSlots[0]!] = 10;
    } else if (correctSlots.length === 2) {
      // both correct -> faster one wins the points
      const faster: Slot = this.answerTimes[0]! <= this.answerTimes[1]! ? 0 : 1;
      deltas[faster] = 10;
    }

    this.scores[0] += deltas[0];
    this.scores[1] += deltas[1];

    for (const slot of [0, 1] as Slot[]) {
      this.ctx.sendTo(slot, {
        type: "kzz_result",
        index: this.index,
        correct: q.correct,
        yourAnswer: this.answers[slot],
        opponentAnswer: this.answers[slot === 0 ? 1 : 0],
        deltas,
        scores: this.scores,
      });
    }

    this.timer = setTimeout(() => {
      this.index++;
      if (this.index >= this.questions.length) {
        this.finish();
      } else {
        this.askQuestion();
      }
    }, RESULT_PAUSE_MS);
  }

  private finish(): void {
    if (this.disposed) return;
    recordKoZnaZna(this.ctx.userIds[0], this.correctCount[0], this.missedCount[0]);
    recordKoZnaZna(this.ctx.userIds[1], this.correctCount[1], this.missedCount[1]);
    this.ctx.complete([this.scores[0], this.scores[1]]);
  }

  dispose(): void {
    this.disposed = true;
    if (this.timer) clearTimeout(this.timer);
    this.timer = null;
  }
}
