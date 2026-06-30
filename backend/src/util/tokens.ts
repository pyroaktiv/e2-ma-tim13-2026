import { db } from "../db/database";

/**
 * Mehanizam tokena (spec 3.a): 5 tokena dnevno + ligaški bonus (spec 6.b) i 1 token po partiji.
 *
 * Ligaški bonus: svaka liga iznad nulte donosi po dodatni token dnevno (npr. 3. liga -> +3),
 * pa je dnevna dodela 5 + (league_id - 1).
 */
export const DAILY_TOKENS = 5;

function today(): string {
  return new Date().toISOString().slice(0, 10);
}

/** Dodaje dnevne tokene (5 + ligaški bonus) ako danas još nisu dodeljeni. */
export function grantDailyTokensIfDue(userId: number): void {
  const row = db
    .query("SELECT last_token_grant, league_id FROM users WHERE id = ?")
    .get(userId) as { last_token_grant: string | null; league_id: number } | null;
  if (!row) return;
  if (row.last_token_grant === today()) return;

  const leagueBonus = Math.max(0, row.league_id - 1); // nulta liga (id 1) -> 0 bonusa
  db.query("UPDATE users SET tokens = tokens + ?, last_token_grant = ? WHERE id = ?").run(
    DAILY_TOKENS + leagueBonus,
    today(),
    userId,
  );
}

/** Troši 1 token za pokretanje partije. Vraća false ako igrač nema tokena. */
export function spendMatchToken(userId: number): boolean {
  const result = db
    .query("UPDATE users SET tokens = tokens - 1 WHERE id = ? AND tokens > 0")
    .run(userId);
  return result.changes > 0;
}

export function refundMatchToken(userId: number): void {
  db.query("UPDATE users SET tokens = tokens + 1 WHERE id = ?").run(userId);
}

export function hasToken(userId: number): boolean {
  const row = db.query("SELECT tokens FROM users WHERE id = ?").get(userId) as
    | { tokens: number }
    | null;
  return !!row && row.tokens > 0;
}
