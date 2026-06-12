// Shared contracts between the match coordinator and the individual games.

export type Slot = 0 | 1;
export type Scores = [number, number];

// A single real-time game (Ko zna zna, Spojnice, ...) talks to the match
// through this context: it sends messages and reports the final score.
export interface GameContext {
  readonly userIds: [number, number];
  readonly usernames: [string, string];
  // Send a payload to one player.
  sendTo(slot: Slot, payload: object): void;
  // Send a payload to both players.
  broadcast(payload: object): void;
  // Called exactly once by the game when it is finished, with the points each
  // player scored in this game.
  complete(scores: Scores): void;
}

export interface Game {
  start(): void;
  handleMessage(slot: Slot, msg: GameMessage): void;
  // A player disconnected / abandoned. The game just needs to stop its timers;
  // the match decides the overall outcome.
  dispose(): void;
}

export interface GameMessage {
  type: string;
  [key: string]: unknown;
}

export const other = (slot: Slot): Slot => (slot === 0 ? 1 : 0);
