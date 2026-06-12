// Dev seed: creates two pre-verified test accounts (so you can log in without
// going through email verification) and makes them friends. Run with:
//   bun run seed.ts
import { initDb } from "./src/db/schema";
import { db } from "./src/db/database";

initDb();

interface SeedUser {
  email: string;
  username: string;
  password: string;
  region: string;
}

const users: SeedUser[] = [
  { email: "pera@test.com", username: "pera", password: "Pera1234", region: "Vojvodina" },
  { email: "mika@test.com", username: "mika", password: "Mika1234", region: "Sumadija" },
];

for (const u of users) {
  const existing = db
    .query("SELECT id FROM users WHERE username = ?")
    .get(u.username) as { id: number } | null;

  if (existing) {
    db.query(
      "UPDATE users SET email_verified = 1, tokens = 5 WHERE id = ?",
    ).run(existing.id);
    console.log(`Updated existing user '${u.username}' (id=${existing.id})`);
    continue;
  }

  const passwordHash = await Bun.password.hash(u.password);
  const qrToken = crypto.randomUUID();
  const { lastInsertRowid } = db
    .query(
      `INSERT INTO users (email, username, password_hash, region, qr_token, email_verified, tokens, total_stars)
       VALUES (?, ?, ?, ?, ?, 1, 5, 0)`,
    )
    .run(u.email, u.username, passwordHash, u.region, qrToken);
  db.query("INSERT OR IGNORE INTO match_summary (user_id) VALUES (?)").run(
    lastInsertRowid,
  );
  console.log(`Created user '${u.username}' (id=${lastInsertRowid})`);
}

// Make the two seeded users friends (so friend game-invites also work).
const ids = users.map(
  (u) =>
    (db.query("SELECT id FROM users WHERE username = ?").get(u.username) as {
      id: number;
    }).id,
);
if (ids.length === 2 && ids[0] != null && ids[1] != null) {
  db.query(
    `INSERT OR IGNORE INTO friend_requests (from_user_id, to_user_id, status)
     VALUES (?, ?, 'accepted')`,
  ).run(ids[0], ids[1]);
}

console.log("\nSeed complete. Login credentials:");
for (const u of users) console.log(`  ${u.username} / ${u.password}`);
