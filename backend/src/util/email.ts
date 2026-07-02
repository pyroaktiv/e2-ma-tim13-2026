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
  const link = `${process.env.BASE_URL}/api/auth/verify-email?token=${token}`;
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
