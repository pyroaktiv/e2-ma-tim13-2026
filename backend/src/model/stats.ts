export type GameType =
  | "ko_zna_zna"
  | "moj_broj"
  | "korak_po_korak"
  | "asocijacije"
  | "skocko"
  | "spojnice";

export interface KoZnaZnaStats {
  correct: number;
  missed: number;
}

export interface MojBrojStats {
  total_attempts: number;
  exact_hits: number;
}

export interface KorakPoKorakStats {
  guessed_at_step: [number, number, number, number, number, number, number];
  failed: number;
}

export interface AsocijacijeStats {
  solved: number;
  unsolved: number;
}

export interface SkockoStats {
  correct_at_attempt: [number, number, number, number, number, number];
  failed: number;
}

export interface SpojniceStats {
  total: number;
  successful: number;
}

export interface StatsResponse {
  overall: {
    total_games: number;
    wins: number;
    losses: number;
    win_ratio: number;
  };
  ko_zna_zna: KoZnaZnaStats;
  moj_broj: MojBrojStats;
  korak_po_korak: KorakPoKorakStats;
  asocijacije: AsocijacijeStats;
  skocko: SkockoStats;
  spojnice: SpojniceStats;
}
