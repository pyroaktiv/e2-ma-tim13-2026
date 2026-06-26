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

  db.run(`
    CREATE TABLE IF NOT EXISTS friend_requests (
      id           INTEGER PRIMARY KEY AUTOINCREMENT,
      from_user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
      to_user_id   INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
      status       TEXT    NOT NULL DEFAULT 'pending'
                   CHECK(status IN ('pending', 'accepted', 'declined')),
      created_at   INTEGER NOT NULL DEFAULT (unixepoch()),
      UNIQUE(from_user_id, to_user_id)
    )
  `);

  db.run(`
    CREATE TABLE IF NOT EXISTS game_invites (
      id           INTEGER PRIMARY KEY AUTOINCREMENT,
      from_user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
      to_user_id   INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
      status       TEXT    NOT NULL DEFAULT 'pending'
                   CHECK(status IN ('pending', 'accepted', 'declined', 'cancelled', 'expired')),
      created_at   INTEGER NOT NULL DEFAULT (unixepoch()),
      expires_at   INTEGER NOT NULL
    )
  `);

  db.run(`
    CREATE TABLE IF NOT EXISTS server_config (
      key   TEXT PRIMARY KEY,
      value TEXT NOT NULL
    )
  `);

  // Sadržaj igara (asocijacije, spojnice, korak po korak, ko zna zna) — spec napomena
  // „podatke ... uneti u bazu". content_json je pool sadržaja za datu igru.
  db.run(`
    CREATE TABLE IF NOT EXISTS game_content (
      id           INTEGER PRIMARY KEY AUTOINCREMENT,
      game         TEXT    NOT NULL UNIQUE,
      content_json TEXT    NOT NULL
    )
  `);

  // Istorija odigranih partija (osnova za rang liste i statistiku).
  db.run(`
    CREATE TABLE IF NOT EXISTS matches (
      id           INTEGER PRIMARY KEY AUTOINCREMENT,
      blue_user_id INTEGER REFERENCES users(id) ON DELETE SET NULL,
      red_user_id  INTEGER REFERENCES users(id) ON DELETE SET NULL,
      blue_score   INTEGER NOT NULL,
      red_score    INTEGER NOT NULL,
      winner       TEXT    NOT NULL CHECK(winner IN ('blue', 'red', 'draw')),
      is_ranked    INTEGER NOT NULL DEFAULT 1,
      created_at   INTEGER NOT NULL DEFAULT (unixepoch())
    )
  `);

  // Izazov (spec 9): lobby koji domaćin pokreće, do 4 učesnika igraju samostalno isti sadržaj.
  db.run(`
    CREATE TABLE IF NOT EXISTS challenges (
      id           TEXT    PRIMARY KEY,
      creator_id   INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
      stake_stars  INTEGER NOT NULL,
      stake_tokens INTEGER NOT NULL,
      status       TEXT    NOT NULL DEFAULT 'open' CHECK(status IN ('open', 'active', 'finished', 'cancelled')),
      created_at   INTEGER NOT NULL DEFAULT (unixepoch()),
      started_at   INTEGER,
      finished_at  INTEGER
    )
  `);

  db.run(`
    CREATE TABLE IF NOT EXISTS monthly_region_stats (
      region_name TEXT    NOT NULL,
      year        INTEGER NOT NULL,
      month       INTEGER NOT NULL,
      total_stars INTEGER NOT NULL,
      rank        INTEGER NOT NULL,
      PRIMARY KEY (region_name, year, month)
    )
  `);

  db.run(`
    CREATE TABLE IF NOT EXISTS challenge_participants (
      challenge_id  TEXT    NOT NULL REFERENCES challenges(id) ON DELETE CASCADE,
      user_id       INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
      joined_at     INTEGER NOT NULL DEFAULT (unixepoch()),
      score         INTEGER,
      reward_stars  INTEGER,
      reward_tokens INTEGER,
      PRIMARY KEY (challenge_id, user_id)
    )
  `);

  // Čet (spec 8): poruke između dva korisnika istog regiona; konverzacija postaje
  // vidljiva čim postoji bar jedna poruka u bilo kom smeru.
  db.run(`
    CREATE TABLE IF NOT EXISTS chat_messages (
      id           INTEGER PRIMARY KEY AUTOINCREMENT,
      from_user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
      to_user_id   INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
      body         TEXT    NOT NULL,
      is_read      INTEGER NOT NULL DEFAULT 0,
      created_at   INTEGER NOT NULL DEFAULT (unixepoch())
    )
  `);

  try { db.run("ALTER TABLE users ADD COLUMN in_game                INTEGER NOT NULL DEFAULT 0"); } catch {}
  try { db.run("ALTER TABLE users ADD COLUMN weekly_stars           INTEGER NOT NULL DEFAULT 0"); } catch {}
  try { db.run("ALTER TABLE users ADD COLUMN weekly_games           INTEGER NOT NULL DEFAULT 0"); } catch {}
  try { db.run("ALTER TABLE users ADD COLUMN monthly_stars          INTEGER NOT NULL DEFAULT 0"); } catch {}
  try { db.run("ALTER TABLE users ADD COLUMN monthly_games          INTEGER NOT NULL DEFAULT 0"); } catch {}
  try { db.run("ALTER TABLE users ADD COLUMN avatar_frame           TEXT    NOT NULL DEFAULT 'none'"); } catch {}
  try { db.run("ALTER TABLE users ADD COLUMN map_lat                REAL"); } catch {}
  try { db.run("ALTER TABLE users ADD COLUMN map_lng                REAL"); } catch {}
  try { db.run("ALTER TABLE users ADD COLUMN last_token_grant       TEXT"); } catch {}
  try { db.run("ALTER TABLE users ADD COLUMN stars_token_progress   INTEGER NOT NULL DEFAULT 0"); } catch {}

  db.run(`
    CREATE TABLE IF NOT EXISTS user_daily_missions (
      user_id     INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
      mission_key TEXT    NOT NULL
                  CHECK(mission_key IN ('win_match', 'send_chat', 'friendly_match', 'win_tournament')),
      date        TEXT    NOT NULL,
      completed   INTEGER NOT NULL DEFAULT 0,
      PRIMARY KEY (user_id, mission_key, date)
    )
  `);

  db.run(`
    CREATE TABLE IF NOT EXISTS user_daily_bonus (
      user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
      date    TEXT    NOT NULL,
      PRIMARY KEY (user_id, date)
    )
  `);

  db.run("CREATE INDEX IF NOT EXISTS idx_daily_missions_user ON user_daily_missions(user_id, date)");

  db.run("CREATE INDEX IF NOT EXISTS idx_users_email         ON users(email)");
  db.run("CREATE INDEX IF NOT EXISTS idx_users_username      ON users(username)");
  db.run("CREATE INDEX IF NOT EXISTS idx_email_ver_token     ON email_verifications(token)");
  db.run("CREATE INDEX IF NOT EXISTS idx_game_stats_user     ON game_stats(user_id)");
  db.run("CREATE INDEX IF NOT EXISTS idx_revoked_exp         ON revoked_tokens(expires_at)");
  db.run("CREATE INDEX IF NOT EXISTS idx_notifications_user  ON notifications(user_id, created_at DESC)");
  db.run("CREATE INDEX IF NOT EXISTS idx_friend_req_from     ON friend_requests(from_user_id)");
  db.run("CREATE INDEX IF NOT EXISTS idx_friend_req_to       ON friend_requests(to_user_id)");
  db.run("CREATE INDEX IF NOT EXISTS idx_game_invites_to     ON game_invites(to_user_id)");
  db.run("CREATE INDEX IF NOT EXISTS idx_game_invites_from   ON game_invites(from_user_id)");
  db.run("CREATE INDEX IF NOT EXISTS idx_users_weekly_stars  ON users(weekly_stars  DESC) WHERE weekly_games  > 0");
  db.run("CREATE INDEX IF NOT EXISTS idx_users_monthly_stars ON users(monthly_stars DESC) WHERE monthly_games > 0");
  db.run("CREATE INDEX IF NOT EXISTS idx_challenges_status   ON challenges(status, created_at DESC)");
  db.run("CREATE INDEX IF NOT EXISTS idx_challenge_part_chal ON challenge_participants(challenge_id)");
  db.run("CREATE INDEX IF NOT EXISTS idx_chat_from_to        ON chat_messages(from_user_id, to_user_id, created_at)");
  db.run("CREATE INDEX IF NOT EXISTS idx_chat_to_from        ON chat_messages(to_user_id, from_user_id, created_at)");

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

  const now = new Date();
  const currentMonth = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, "0")}`;
  const d = new Date(Date.UTC(now.getFullYear(), now.getMonth(), now.getDate()));
  const dayNum = d.getUTCDay() || 7;
  d.setUTCDate(d.getUTCDate() + 4 - dayNum);
  const yearStart = new Date(Date.UTC(d.getUTCFullYear(), 0, 1));
  const weekNo = Math.ceil((((d.getTime() - yearStart.getTime()) / 86400000) + 1) / 7);
  const currentWeek = `${d.getUTCFullYear()}-${String(weekNo).padStart(2, "0")}`;

  db.run("INSERT OR IGNORE INTO server_config (key, value) VALUES ('last_weekly_reset',  ?)", [currentWeek]);
  db.run("INSERT OR IGNORE INTO server_config (key, value) VALUES ('last_monthly_reset', ?)", [currentMonth]);
}
