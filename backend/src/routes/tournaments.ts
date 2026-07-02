import { db } from "../db/database";
import { requireAuth } from "../middleware/auth";
import { json } from "../util/response";

export async function handleGetTournament(req: Request): Promise<Response> {
  const auth = await requireAuth(req);
  if (auth instanceof Response) return auth;

  const url = new URL(req.url);
  const id = url.pathname.split("/").at(-1);
  if (!id) return json({ error: "Missing tournament id" }, 400);

  const tournament = db.query("SELECT id, status, semi1_room_id, semi2_room_id, final_room_id, created_at, finished_at FROM tournaments WHERE id = ?").get(id) as {
    id: string;
    status: string;
    semi1_room_id: string | null;
    semi2_room_id: string | null;
    final_room_id: string | null;
    created_at: number;
    finished_at: number | null;
  } | null;

  if (!tournament) return json({ error: "Tournament not found" }, 404);

  const participants = db.query(`
    SELECT tp.user_id, tp.semi_index, tp.result, tp.score,
           u.username, u.avatar, l.name AS league_name, l.icon AS league_icon
    FROM tournament_participants tp
    LEFT JOIN users u ON u.id = tp.user_id
    LEFT JOIN leagues l ON l.id = u.league_id
    WHERE tp.tournament_id = ?
    ORDER BY tp.semi_index, tp.user_id
  `).all(id) as Array<{
    user_id: number;
    semi_index: number;
    result: string | null;
    score: number | null;
    username: string;
    avatar: string;
    league_name: string;
    league_icon: string;
  }>;

  return json({
    id: tournament.id,
    status: tournament.status,
    semi1_room_id: tournament.semi1_room_id,
    semi2_room_id: tournament.semi2_room_id,
    final_room_id: tournament.final_room_id,
    created_at: tournament.created_at,
    finished_at: tournament.finished_at,
    participants: participants.map((p) => ({
      userId: p.user_id,
      username: p.username,
      avatar: p.avatar,
      league: { name: p.league_name, icon: p.league_icon },
      semiIndex: p.semi_index,
      result: p.result,
      score: p.score,
    })),
  });
}
