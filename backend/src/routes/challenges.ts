import { db } from "../db/database";
import { requireAuth } from "../middleware/auth";
import { json } from "../util/response";
import type { ChallengeStatus } from "../challenge/types";

interface ChallengeRow {
  id: string;
  creator_id: number;
  creator_username: string;
  stake_stars: number;
  stake_tokens: number;
  status: ChallengeStatus;
}

interface ParticipantRow {
  user_id: number;
  username: string;
  score: number | null;
  reward_stars: number | null;
  reward_tokens: number | null;
}

/**
 * REST odgovori u ovom backendu su snake_case (vidi npr. routes/user/profile.ts) — Android
 * Retrofit klijent koristi Gson sa LOWER_CASE_WITH_UNDERSCORES politikom. Socket poruke
 * (challenge/manager.ts) su, nasuprot tome, camelCase (kao i ostale socket poruke), jer ih
 * SocketManager parsira običnim Gson-om bez te politike. Zato ovde NE recikliramo camelCase
 * `ChallengeDto` iz challenge/types.ts, već ručno serijalizujemo snake_case oblik.
 */
function toRestDto(row: ChallengeRow): object {
  const participants = db
    .query(
      `SELECT cp.user_id, u.username, cp.score, cp.reward_stars, cp.reward_tokens
       FROM challenge_participants cp
       JOIN users u ON u.id = cp.user_id
       WHERE cp.challenge_id = ?
       ORDER BY cp.joined_at ASC`,
    )
    .all(row.id) as ParticipantRow[];

  return {
    id: row.id,
    creator_id: row.creator_id,
    creator_username: row.creator_username,
    stake_stars: row.stake_stars,
    stake_tokens: row.stake_tokens,
    status: row.status,
    participants: participants.map((p) => ({
      user_id: p.user_id,
      username: p.username,
      score: p.score,
      reward_stars: p.reward_stars,
      reward_tokens: p.reward_tokens,
    })),
  };
}

/** Lista otvorenih izazova kojima se može priključiti (spec 9.a). */
export async function handleListChallenges(req: Request): Promise<Response> {
  const auth = await requireAuth(req);
  if (auth instanceof Response) return auth;

  const rows = db
    .query(
      `SELECT c.id, c.creator_id, u.username AS creator_username, c.stake_stars, c.stake_tokens, c.status
       FROM challenges c
       JOIN users u ON u.id = c.creator_id
       WHERE c.status = 'open'
       ORDER BY c.created_at DESC`,
    )
    .all() as ChallengeRow[];

  return json(rows.map(toRestDto));
}

export async function handleGetChallenge(req: Request): Promise<Response> {
  const auth = await requireAuth(req);
  if (auth instanceof Response) return auth;

  const id = new URL(req.url).pathname.split("/")[3];
  if (!id) return json({ error: "Challenge not found" }, 404);

  const row = db
    .query(
      `SELECT c.id, c.creator_id, u.username AS creator_username, c.stake_stars, c.stake_tokens, c.status
       FROM challenges c
       JOIN users u ON u.id = c.creator_id
       WHERE c.id = ?`,
    )
    .get(id) as ChallengeRow | null;

  if (!row) return json({ error: "Challenge not found" }, 404);
  return json(toRestDto(row));
}
