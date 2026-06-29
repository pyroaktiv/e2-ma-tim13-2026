import type { Participant } from "../match/types";

export interface TournamentRewards {
  starsDelta: number;
  tokensDelta: number;
  totalStars: number;
  tokens: number;
  league: string;
}

export interface TournamentSemiState {
  roomId: string;
  players: [Participant, Participant];
  winnerId: number | null;
  winnerScore: number;
  loserId: number | null;
  loserScore: number;
  done: boolean;
}

export interface ActiveTournament {
  id: string;
  status: "semifinal" | "final" | "finished";
  allParticipants: Participant[];
  semis: [TournamentSemiState, TournamentSemiState];
  finalRoomId: string | null;
}

export type TournamentPhase = "semi" | "final";
