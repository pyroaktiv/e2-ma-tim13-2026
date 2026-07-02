import { z } from "zod";
import { db } from "../../db/database";
import { requireAuth } from "../../middleware/auth";
import { json } from "../../util/response";

const VALID_AVATARS = Array.from({ length: 10 }, (_, i) => `avatar_0${i + 1}`);

const AvatarSchema = z.object({
  avatar: z
    .string()
    .refine((v) => VALID_AVATARS.includes(v) || v === "default", {
      message: "Invalid avatar selection",
    }),
});

export async function handleUpdateAvatar(req: Request): Promise<Response> {
  const auth = await requireAuth(req);
  if (auth instanceof Response) return auth;

  let body: unknown;
  try {
    body = await req.json();
  } catch {
    return json({ error: "Invalid JSON" }, 400);
  }

  const result = AvatarSchema.safeParse(body);
  if (!result.success) {
    return json({ error: result.error.issues[0]?.message }, 400);
  }

  db.query(
    "UPDATE users SET avatar = ?, updated_at = unixepoch() WHERE id = ?",
  ).run(result.data.avatar, auth.user_id);

  return json({ message: "Avatar updated.", avatar: result.data.avatar });
}
