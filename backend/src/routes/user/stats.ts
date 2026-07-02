import { db } from "../../db/database";
import { requireAuth } from "../../middleware/auth";
import { json } from "../../util/response";
import type {
  StatsResponse,
  GameType,
  KoZnaZnaStats,
  MojBrojStats,
  KorakPoKorakStats,
  AsocijacijeStats,
  SkockoStats,
  SpojniceStats,
} from "../../model/stats";

const DEFAULT_STATS = {
  ko_zna_zna: { correct: 0, missed: 0 } as KoZnaZnaStats,
  moj_broj: { total_attempts: 0, exact_hits: 0 } as MojBrojStats,
  korak_po_korak: { guessed_at_step: [0, 0, 0, 0, 0, 0, 0], failed: 0 } as KorakPoKorakStats,
  asocijacije: { solved: 0, unsolved: 0 } as AsocijacijeStats,
  skocko: { correct_at_attempt: [0, 0, 0, 0, 0, 0], failed: 0 } as SkockoStats,
  spojnice: { total: 0, successful: 0 } as SpojniceStats,
};

export async function handleGetStats(req: Request): Promise<Response> {
  const auth = await requireAuth(req);
  if (auth instanceof Response) return auth;

  const summary = db
    .query("SELECT total_games, wins, losses FROM match_summary WHERE user_id = ?")
    .get(auth.user_id) as { total_games: number; wins: number; losses: number } | null;

  const totalGames = summary?.total_games ?? 0;
  const wins = summary?.wins ?? 0;
  const losses = summary?.losses ?? 0;

  const rows = db
    .query("SELECT game, stats_json FROM game_stats WHERE user_id = ?")
    .all(auth.user_id) as Array<{ game: GameType; stats_json: string }>;

  const gameMap = new Map(rows.map((r) => [r.game, JSON.parse(r.stats_json)]));

  const response: StatsResponse = {
    overall: {
      total_games: totalGames,
      wins,
      losses,
      win_ratio: totalGames > 0 ? wins / totalGames : 0,
    },
    ko_zna_zna: (gameMap.get("ko_zna_zna") as KoZnaZnaStats) ?? DEFAULT_STATS.ko_zna_zna,
    moj_broj: (gameMap.get("moj_broj") as MojBrojStats) ?? DEFAULT_STATS.moj_broj,
    korak_po_korak:
      (gameMap.get("korak_po_korak") as KorakPoKorakStats) ?? DEFAULT_STATS.korak_po_korak,
    asocijacije: (gameMap.get("asocijacije") as AsocijacijeStats) ?? DEFAULT_STATS.asocijacije,
    skocko: (gameMap.get("skocko") as SkockoStats) ?? DEFAULT_STATS.skocko,
    spojnice: (gameMap.get("spojnice") as SpojniceStats) ?? DEFAULT_STATS.spojnice,
  };

  return json(response);
}
