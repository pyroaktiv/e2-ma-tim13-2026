import { db } from "../db/database";
import { verifyJWT } from "../util/jwt";
import { json } from "../util/response";

export interface AuthContext {
  user_id: number;
  username: string;
  jti: string;
}

export async function requireAuth(req: Request): Promise<AuthContext | Response> {
  const authHeader = req.headers.get("Authorization");
  if (!authHeader?.startsWith("Bearer ")) {
    return json({ error: "Unauthorized" }, 401);
  }

  const token = authHeader.slice(7);
  const payload = await verifyJWT(token);
  if (!payload) {
    return json({ error: "Invalid or expired token" }, 401);
  }

  const revoked = db.query("SELECT 1 FROM revoked_tokens WHERE jti = ?").get(payload.jti);
  if (revoked) {
    return json({ error: "Token revoked" }, 401);
  }

  return { user_id: Number(payload.sub), username: payload.username, jti: payload.jti };
}
