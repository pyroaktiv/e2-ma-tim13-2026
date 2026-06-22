import type { PerGameStats } from "../match/types";

export type ChallengeStatus = "open" | "active" | "finished" | "cancelled";

export const MAX_CHALLENGE_PARTICIPANTS = 4;
export const MIN_PARTICIPANTS_TO_MANUAL_START = 2;
export const MAX_STAKE_STARS = 10;
export const MAX_STAKE_TOKENS = 2;

export interface ChallengeParticipantDto {
  userId: number;
  username: string;
  score: number | null;
  rewardStars: number | null;
  rewardTokens: number | null;
}

/** Oblik koji klijent dobija i preko REST liste i preko `challenge_update` poruke. */
export interface ChallengeDto {
  id: string;
  creatorId: number;
  creatorUsername: string;
  stakeStars: number;
  stakeTokens: number;
  status: ChallengeStatus;
  participants: ChallengeParticipantDto[];
}

/** Poruke koje klijent šalje serveru za tok izazova (spec 9). */
export type ChallengeClientMsg =
  | { type: "create_challenge"; stars: number; tokens: number }
  | { type: "join_challenge"; challengeId: string }
  | { type: "leave_challenge"; challengeId: string }
  | { type: "start_challenge"; challengeId: string }
  | { type: "report_challenge_result"; challengeId: string; score: number; perGame: PerGameStats[] };
