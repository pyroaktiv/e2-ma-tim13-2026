import { db } from "../../db/database";
import { requireAuth } from "../../middleware/auth";
import { verifyJWT } from "../../util/jwt";
import { json } from "../../util/response";

export async function handleLogout(req: Request): Promise<Response> {
  const auth = await requireAuth(req);
  if (auth instanceof Response) return auth;

  const token = req.headers.get("Authorization")!.slice(7);
  const payload = await verifyJWT(token);
  if (!payload?.exp) return json({ message: "Logged out." });

  db.query("INSERT OR IGNORE INTO revoked_tokens (jti, expires_at) VALUES (?, ?)").run(
    auth.jti,
    payload.exp
  );

  return json({ message: "Logged out." });
}
