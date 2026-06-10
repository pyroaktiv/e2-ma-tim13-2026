import { db } from "./database";

export function initDb(): void {
  db.run(`
    CREATE TABLE IF NOT EXISTS leagues (
      id        INTEGER PRIMARY KEY,
      name      TEXT    NOT NULL UNIQUE,
      min_stars INTEGER NOT NULL,
      icon      TEXT    NOT NULL
    )
  `);

  db.run(`
    CREATE TABLE IF NOT EXISTS users (
      id             INTEGER PRIMARY KEY AUTOINCREMENT,
      email          TEXT    NOT NULL UNIQUE COLLATE NOCASE,
      username       TEXT    NOT NULL UNIQUE COLLATE NOCASE,
      password_hash  TEXT    NOT NULL,
      region         TEXT    NOT NULL,
      avatar         TEXT    NOT NULL DEFAULT 'default',
      tokens         INTEGER NOT NULL DEFAULT 0,
      total_stars    INTEGER NOT NULL DEFAULT 0,
      league_id      INTEGER NOT NULL DEFAULT 1 REFERENCES leagues(id),
      qr_token       TEXT    NOT NULL UNIQUE,
      email_verified INTEGER NOT NULL DEFAULT 0,
      created_at     INTEGER NOT NULL DEFAULT (unixepoch()),
      updated_at     INTEGER NOT NULL DEFAULT (unixepoch())
    )
  `);

  db.run(`
    CREATE TABLE IF NOT EXISTS email_verifications (
      id         INTEGER PRIMARY KEY AUTOINCREMENT,
      user_id    INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
      token      TEXT    NOT NULL UNIQUE,
      expires_at INTEGER NOT NULL,
      used       INTEGER NOT NULL DEFAULT 0
    )
  `);

  db.run(`
    CREATE TABLE IF NOT EXISTS revoked_tokens (
      jti        TEXT    PRIMARY KEY,
      expires_at INTEGER NOT NULL
    )
  `);

  db.run(`
    CREATE TABLE IF NOT EXISTS game_stats (
      id         INTEGER PRIMARY KEY AUTOINCREMENT,
      user_id    INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
      game       TEXT    NOT NULL,
      stats_json TEXT    NOT NULL DEFAULT '{}',
      updated_at INTEGER NOT NULL DEFAULT (unixepoch()),
      UNIQUE(user_id, game)
    )
  `);

  db.run(`
    CREATE TABLE IF NOT EXISTS match_summary (
      user_id     INTEGER PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
      total_games INTEGER NOT NULL DEFAULT 0,
      wins        INTEGER NOT NULL DEFAULT 0,
      losses      INTEGER NOT NULL DEFAULT 0,
      updated_at  INTEGER NOT NULL DEFAULT (unixepoch())
    )
  `);

  db.run(`
    CREATE TABLE IF NOT EXISTS notifications (
      id         INTEGER PRIMARY KEY AUTOINCREMENT,
      user_id    INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
      category   TEXT    NOT NULL CHECK(category IN ('CET', 'RANGIRANJE', 'NAGRADE', 'OSTALO')),
      title      TEXT    NOT NULL,
      body       TEXT    NOT NULL,
      is_read    INTEGER NOT NULL DEFAULT 0,
      created_at INTEGER NOT NULL DEFAULT (unixepoch())
    )
  `);

  db.run("CREATE INDEX IF NOT EXISTS idx_users_email      ON users(email)");
  db.run("CREATE INDEX IF NOT EXISTS idx_users_username   ON users(username)");
  db.run("CREATE INDEX IF NOT EXISTS idx_email_ver_token  ON email_verifications(token)");
  db.run("CREATE INDEX IF NOT EXISTS idx_game_stats_user  ON game_stats(user_id)");
  db.run("CREATE INDEX IF NOT EXISTS idx_revoked_exp      ON revoked_tokens(expires_at)");
  db.run("CREATE INDEX IF NOT EXISTS idx_notifications_user ON notifications(user_id, created_at DESC)");

  const seedLeagues = db.transaction(() => {
    const leagues = [
      { id: 1, name: "Bronze",   min_stars: 0,    icon: "league_bronze" },
      { id: 2, name: "Silver",   min_stars: 100,  icon: "league_silver" },
      { id: 3, name: "Gold",     min_stars: 300,  icon: "league_gold" },
      { id: 4, name: "Platinum", min_stars: 600,  icon: "league_platinum" },
      { id: 5, name: "Diamond",  min_stars: 1000, icon: "league_diamond" },
      { id: 6, name: "Master",   min_stars: 1500, icon: "league_master" },
    ];
    const stmt = db.prepare(
      "INSERT OR IGNORE INTO leagues (id, name, min_stars, icon) VALUES (?, ?, ?, ?)"
    );
    for (const l of leagues) stmt.run(l.id, l.name, l.min_stars, l.icon);
  });

  seedLeagues();
}
