import { z } from "zod";
import { db } from "../../db/database";
import { signJWT } from "../../util/jwt";
import { json } from "../../util/response";
import { grantDailyTokensIfDue } from "../../util/tokens";
import type { UserRow } from "../../model/user";

const LoginSchema = z.object({
  identifier: z.string().min(1, "Email or username is required"),
  password: z.string().min(1, "Password is required"),
});

export async function handleLogin(req: Request): Promise<Response> {
  let body: unknown;
  try {
    body = await req.json();
  } catch {
    return json({ error: "Invalid JSON" }, 400);
  }

  const result = LoginSchema.safeParse(body);
  if (!result.success) {
    return json({ error: result.error.issues[0]?.message }, 400);
  }

  const { identifier, password } = result.data;

  const isEmail = identifier.includes("@");
  const user = db
    .query(
      isEmail
        ? "SELECT * FROM users WHERE email = ?"
        : "SELECT * FROM users WHERE username = ?",
    )
    .get(identifier) as UserRow | null;

  if (!user) {
    return json({ error: "Invalid credentials" }, 401);
  }

  const passwordValid = await Bun.password.verify(password, user.password_hash);
  if (!passwordValid) {
    return json({ error: "Invalid credentials" }, 401);
  }

  if (!user.email_verified) {
    return json({ error: "Email not verified" }, 403);
  }

  // Spec 3.a: dnevnih 5 tokena (ako danas još nisu dodeljeni).
  grantDailyTokensIfDue(user.id);

  const token = await signJWT(user.id, user.username);

  return json({
    token,
    user: {
      id: user.id,
      username: user.username,
      email: user.email,
      avatar: user.avatar,
    },
  });
}
