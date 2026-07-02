import { z } from "zod";
import { db } from "../../db/database";
import { requireAuth } from "../../middleware/auth";
import { json } from "../../util/response";
import type { UserRow } from "../../model/user";

const ResetPasswordSchema = z
  .object({
    old_password: z.string().min(1, "Current password is required"),
    new_password: z
      .string()
      .min(8, "New password must be at least 8 characters")
      .regex(/[A-Z]/, "New password must contain at least one uppercase letter")
      .regex(/[0-9]/, "New password must contain at least one digit"),
    new_password_confirm: z.string(),
  })
  .refine((d) => d.new_password === d.new_password_confirm, {
    message: "New passwords do not match",
    path: ["new_password_confirm"],
  });

export async function handleResetPassword(req: Request): Promise<Response> {
  const auth = await requireAuth(req);
  if (auth instanceof Response) return auth;

  let body: unknown;
  try {
    body = await req.json();
  } catch {
    return json({ error: "Invalid JSON" }, 400);
  }

  const result = ResetPasswordSchema.safeParse(body);
  if (!result.success) {
    return json({ error: result.error.issues[0]?.message }, 400);
  }

  const { old_password, new_password } = result.data;

  const user = db
    .query("SELECT password_hash FROM users WHERE id = ?")
    .get(auth.user_id) as Pick<UserRow, "password_hash"> | null;

  if (!user) return json({ error: "User not found" }, 404);

  const valid = await Bun.password.verify(old_password, user.password_hash);
  if (!valid) {
    return json({ error: "Current password is incorrect" }, 401);
  }

  const newHash = await Bun.password.hash(new_password);
  db.query(
    "UPDATE users SET password_hash = ?, updated_at = unixepoch() WHERE id = ?",
  ).run(newHash, auth.user_id);

  return json({ message: "Password updated successfully." });
}
