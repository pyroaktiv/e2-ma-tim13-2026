import { db } from "../../db/database";
import { json } from "../../util/response";

export function handleVerifyEmail(req: Request): Response {
  const url = new URL(req.url);
  const token = url.searchParams.get("token");

  if (!token) {
    return json({ error: "Missing token" }, 400);
  }

  const now = Math.floor(Date.now() / 1000);
  const record = db
    .query(
      "SELECT id, user_id FROM email_verifications WHERE token = ? AND used = 0 AND expires_at > ?"
    )
    .get(token, now) as { id: number; user_id: number } | null;

  if (!record) {
    return json({ error: "Invalid or expired verification token" }, 400);
  }

  db.query("UPDATE email_verifications SET used = 1 WHERE id = ?").run(record.id);
  db.query("UPDATE users SET email_verified = 1, updated_at = unixepoch() WHERE id = ?").run(
    record.user_id
  );

  return json({ message: "Email verified. You can now log in." });
}
