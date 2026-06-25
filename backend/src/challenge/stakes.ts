import { db } from "../db/database";

/** Eskrou zvezdi/tokena uloženih u izazov (spec 9.b/c) — odvojeno od regularnih nagrada partije. */
export function hasStake(userId: number, stars: number, tokens: number): boolean {
  const row = db.query("SELECT total_stars, tokens FROM users WHERE id = ?").get(userId) as
    | { total_stars: number; tokens: number }
    | null;
  return !!row && row.total_stars >= stars && row.tokens >= tokens;
}

/** Skida ulog pri kreiranju/prijavi na izazov. Vraća false ako igrač nema dovoljno. */
export function spendStake(userId: number, stars: number, tokens: number): boolean {
  const result = db
    .query(
      "UPDATE users SET total_stars = total_stars - ?, tokens = tokens - ? WHERE id = ? AND total_stars >= ? AND tokens >= ?",
    )
    .run(stars, tokens, userId, stars, tokens);
  return result.changes > 0;
}

/** Vraća ulog (napuštanje pre starta, otkazan izazov, ili „nazad uloženo" za drugo mesto). */
export function refundStake(userId: number, stars: number, tokens: number): void {
  db.query("UPDATE users SET total_stars = total_stars + ?, tokens = tokens + ? WHERE id = ?").run(
    stars,
    tokens,
    userId,
  );
}

/** Dodaje osvojenu nagradu (75% uloga za pobednika izazova). */
export function creditReward(userId: number, stars: number, tokens: number): void {
  db.query("UPDATE users SET total_stars = total_stars + ?, tokens = tokens + ? WHERE id = ?").run(
    stars,
    tokens,
    userId,
  );
}
