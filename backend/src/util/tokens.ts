import { db } from "../db/database";

/**
 * Mehanizam tokena (spec 3.a): 5 tokena dnevno i 1 token po nasumičnoj partiji.
 *
 * NAPOMENA: ligaški bonus tokena (spec 6.b) NIJE ovde — to je tačka 6 (kolega 2).
 */
export const DAILY_TOKENS = 5;

function today(): string {
  return new Date().toISOString().slice(0, 10);
}

/** Dodaje dnevnih 5 tokena ako danas još nisu dodeljeni. Zove se pri loginu i čitanju profila. */
export function grantDailyTokensIfDue(userId: number): void {
  const row = db
    .query("SELECT last_token_grant FROM users WHERE id = ?")
    .get(userId) as { last_token_grant: string | null } | null;
  if (!row) return;
  if (row.last_token_grant === today()) return;

  db.query("UPDATE users SET tokens = tokens + ?, last_token_grant = ? WHERE id = ?").run(
    DAILY_TOKENS,
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
