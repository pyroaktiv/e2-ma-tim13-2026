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

  // Rang liste po ciklusima (spec 4/5/6): zvezde i broj partija koje je igrač osvojio u datom
  // nedeljnom/mesečnom ciklusu. Istorija ciklusa ostaje (za „prethodni ciklus" i statistiku regiona).
  db.run(`
    CREATE TABLE IF NOT EXISTS cycle_scores (
      user_id   INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
      period    TEXT    NOT NULL CHECK(period IN ('weekly', 'monthly')),
      cycle_key TEXT    NOT NULL,
      stars     INTEGER NOT NULL DEFAULT 0,
      games     INTEGER NOT NULL DEFAULT 0,
      PRIMARY KEY (user_id, period, cycle_key)
    )
  `);

  // Generički ključ-vrednost meta podaci (npr. poslednji obrađeni mesečni ciklus za penale liga).
  db.run(`
    CREATE TABLE IF NOT EXISTS app_meta (
      key   TEXT PRIMARY KEY,
      value TEXT NOT NULL
    )
  `);

  try { db.run("ALTER TABLE users ADD COLUMN in_game INTEGER NOT NULL DEFAULT 0"); } catch {}
  // Nasumična tačka igrača na mapi regiona (spec 5.a); popunjava se pri registraciji ili lenjo.
  try { db.run("ALTER TABLE users ADD COLUMN map_lat REAL"); } catch {}
  try { db.run("ALTER TABLE users ADD COLUMN map_lng REAL"); } catch {}
  // Datum poslednje dnevne dodele tokena (YYYY-MM-DD) i napredak ka tokenu od zvezda (50 -> 1).
  try { db.run("ALTER TABLE users ADD COLUMN last_token_grant TEXT"); } catch {}
  try { db.run("ALTER TABLE users ADD COLUMN stars_token_progress INTEGER NOT NULL DEFAULT 0"); } catch {}

  db.run("CREATE INDEX IF NOT EXISTS idx_users_email        ON users(email)");
  db.run("CREATE INDEX IF NOT EXISTS idx_users_username     ON users(username)");
  db.run("CREATE INDEX IF NOT EXISTS idx_email_ver_token    ON email_verifications(token)");
  db.run("CREATE INDEX IF NOT EXISTS idx_game_stats_user    ON game_stats(user_id)");
  db.run("CREATE INDEX IF NOT EXISTS idx_revoked_exp        ON revoked_tokens(expires_at)");
  db.run("CREATE INDEX IF NOT EXISTS idx_notifications_user ON notifications(user_id, created_at DESC)");
  db.run("CREATE INDEX IF NOT EXISTS idx_friend_req_from    ON friend_requests(from_user_id)");
  db.run("CREATE INDEX IF NOT EXISTS idx_friend_req_to      ON friend_requests(to_user_id)");
  db.run("CREATE INDEX IF NOT EXISTS idx_game_invites_to    ON game_invites(to_user_id)");
  db.run("CREATE INDEX IF NOT EXISTS idx_game_invites_from  ON game_invites(from_user_id)");
  db.run("CREATE INDEX IF NOT EXISTS idx_challenges_status  ON challenges(status, created_at DESC)");
  db.run("CREATE INDEX IF NOT EXISTS idx_challenge_part_chal ON challenge_participants(challenge_id)");
  db.run("CREATE INDEX IF NOT EXISTS idx_chat_from_to        ON chat_messages(from_user_id, to_user_id, created_at)");
  db.run("CREATE INDEX IF NOT EXISTS idx_chat_to_from        ON chat_messages(to_user_id, from_user_id, created_at)");
  db.run("CREATE INDEX IF NOT EXISTS idx_cycle_scores_board   ON cycle_scores(period, cycle_key, stars DESC)");

  const seedLeagues = db.transaction(() => {
    // Spec 6.c: nulta liga (0) + 5 liga; prag se računa po formuli prethodni * 2 (100, 200, 400, 800, 1600).
    const leagues = [
      { id: 1, name: "Bronze",   min_stars: 0,    icon: "league_bronze" },
      { id: 2, name: "Silver",   min_stars: 100,  icon: "league_silver" },
      { id: 3, name: "Gold",     min_stars: 200,  icon: "league_gold" },
      { id: 4, name: "Platinum", min_stars: 400,  icon: "league_platinum" },
      { id: 5, name: "Diamond",  min_stars: 800,  icon: "league_diamond" },
      { id: 6, name: "Master",   min_stars: 1600, icon: "league_master" },
    ];
    // Upsert da bi se i postojeće baze ispravile na tačne pragove iz specifikacije.
    const stmt = db.prepare(
      `INSERT INTO leagues (id, name, min_stars, icon) VALUES (?, ?, ?, ?)
       ON CONFLICT(id) DO UPDATE SET name = excluded.name, min_stars = excluded.min_stars, icon = excluded.icon`
    );
    for (const l of leagues) stmt.run(l.id, l.name, l.min_stars, l.icon);
  });

  seedLeagues();
}
