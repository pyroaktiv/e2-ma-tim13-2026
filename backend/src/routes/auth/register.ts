import { z } from "zod";
import { db } from "../../db/database";
import { sendVerificationEmail } from "../../util/email";
import { json } from "../../util/response";
import { REGION_NAMES, generateRandomPoint } from "../../util/regions";

const RegisterSchema = z
  .object({
    email: z.string().email("Invalid email address"),
    username: z
      .string()
      .min(3, "Username must be at least 3 characters")
      .max(30),
    region: z.enum(REGION_NAMES, { message: "Invalid region" }),
    password: z
      .string()
      .min(8, "Password must be at least 8 characters")
      .regex(/[A-Z]/, "Password must contain at least one uppercase letter")
      .regex(/[0-9]/, "Password must contain at least one digit"),
    confirm_password: z.string(),
  })
  .refine((d) => d.password === d.confirm_password, {
    message: "Passwords do not match",
    path: ["confirm_password"],
  });

export async function handleRegister(req: Request): Promise<Response> {
  let body: unknown;
  try {
    body = await req.json();
  } catch {
    return json({ error: "Invalid JSON" }, 400);
  }

  const result = RegisterSchema.safeParse(body);
  if (!result.success) {
    return json({ error: result.error.issues[0]?.message }, 400);
  }

  const { email, username, region, password } = result.data;

  const existing = db
    .query("SELECT id FROM users WHERE email = ? OR username = ?")
    .get(email, username) as { id: number } | null;

  if (existing) {
    const conflict = db
      .query("SELECT email FROM users WHERE email = ?")
      .get(email);
    return json(
      { error: conflict ? "Email already in use" : "Username already taken" },
      409,
    );
  }

  const passwordHash = await Bun.password.hash(password);
  const qrToken = crypto.randomUUID();
  // Spec 5.a: nasumična tačka unutar regiona za prikaz na mapi.
  const { lat, lng } = generateRandomPoint(region);
  const today = new Date().toISOString().slice(0, 10);

  // Spec 3.a: igrač dobija 5 tokena pri registraciji (sledećih 5 stiže narednog dana).
  const { lastInsertRowid: userId } = db
    .query(
      "INSERT INTO users (email, username, password_hash, region, map_lat, map_lng, qr_token, tokens, last_token_grant) VALUES (?, ?, ?, ?, ?, ?, ?, 5, ?)",
    )
    .run(email, username, passwordHash, region, lat, lng, qrToken, today);

  db.query("INSERT INTO match_summary (user_id) VALUES (?)").run(userId);

  // Razvoj bez SMTP-a: preskoči slanje mejla i odmah verifikuj nalog da bi se moglo logovati.
  // U produkciji (kada je SMTP_HOST podešen) ostaje obavezna potvrda mejlom.
  if (!process.env.SMTP_HOST) {
    db.query("UPDATE users SET email_verified = 1 WHERE id = ?").run(userId);
    return json({ message: "Registracija uspešna." }, 201);
  }

  const verifyToken = Buffer.from(
    crypto.getRandomValues(new Uint8Array(32)),
  ).toString("hex");
  const expiresAt = Math.floor(Date.now() / 1000) + 86400;

  db.query(
    "INSERT INTO email_verifications (user_id, token, expires_at) VALUES (?, ?, ?)",
  ).run(userId, verifyToken, expiresAt);

  try {
    await sendVerificationEmail(email, verifyToken);
  } catch (err) {
    console.error("Failed to send verification email:", err);
    return json({ error: "Failed to send verification email" }, 500);
  }

  return json(
    {
      message:
        "Registration successful. Check your email to verify your account.",
    },
    201,
  );
}
