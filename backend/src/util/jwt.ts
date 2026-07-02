import { SignJWT, jwtVerify, type JWTPayload } from "jose";

const secret = new TextEncoder().encode(process.env.JWT_SECRET ?? "changeme");

export interface AppJWTPayload extends JWTPayload {
  sub: string;
  username: string;
  jti: string;
}

export async function signJWT(userId: number, username: string): Promise<string> {
  return new SignJWT({ username })
    .setProtectedHeader({ alg: "HS256" })
    .setSubject(String(userId))
    .setJti(crypto.randomUUID())
    .setIssuedAt()
    .setExpirationTime("24h")
    .sign(secret);
}

export async function verifyJWT(token: string): Promise<AppJWTPayload | null> {
  try {
    const { payload } = await jwtVerify(token, secret);
    return payload as AppJWTPayload;
  } catch {
    return null;
  }
}
