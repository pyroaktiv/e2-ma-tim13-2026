import nodemailer from "nodemailer";

const transporter = nodemailer.createTransport({
  host: process.env.SMTP_HOST,
  port: Number(process.env.SMTP_PORT ?? 587),
  secure: process.env.SMTP_SECURE === "true",
  auth: {
    user: process.env.SMTP_USER,
    pass: process.env.SMTP_PASS,
  },
});

export async function sendVerificationEmail(to: string, token: string): Promise<void> {
  const base = process.env.BASE_URL ?? `http://localhost:${process.env.PORT ?? 3000}`;
  const link = `${base}/api/auth/verify-email?token=${token}`;

  // Dev fallback: if no SMTP is configured, don't try to send a real email —
  // just print the verification link so registration can be tested locally.
  if (!process.env.SMTP_HOST) {
    console.log(`\n[DEV] Verifikacioni link za ${to}:\n${link}\n`);
    return;
  }

  await transporter.sendMail({
    from: `"Slagalica" <${process.env.SMTP_USER}>`,
    to,
    subject: "Potvrdite vašu email adresu",
    html: `
      <p>Hvala što ste se registrovali na Slagalicu!</p>
      <p>Kliknite na link da potvrdite vašu email adresu:</p>
      <a href="${link}">${link}</a>
      <p>Link je važeći 24 sata.</p>
    `,
  });
}
