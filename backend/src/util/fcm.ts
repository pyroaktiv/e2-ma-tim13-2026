import { SignJWT, importPKCS8 } from "jose";
import { db } from "../db/database";

/**
 * FCM (Firebase Cloud Messaging) HTTP v1 slanje (spec 11).
 *
 * Konfiguracija preko env-a (bilo koji od načina):
 *   FCM_SERVICE_ACCOUNT       — JSON string service-account ključa, ili
 *   FCM_SERVICE_ACCOUNT_PATH  — putanja do service-account .json fajla
 *   FCM_PROJECT_ID            — opciono; ako nije zadato, uzima se iz service-account-a
 *
 * Ako ništa nije podešeno, sve funkcije rade kao no-op (dev bez Firebase-a:
 * istorija i in-app WS i dalje rade, samo nema push-a kad je korisnik van aplikacije).
 */

interface ServiceAccount {
  client_email: string;
  private_key: string;
  project_id: string;
}

const TOKEN_ENDPOINT = "https://oauth2.googleapis.com/token";
const SCOPE = "https://www.googleapis.com/auth/firebase.messaging";

let serviceAccount: ServiceAccount | null = null;
let loaded = false;

function loadServiceAccount(): ServiceAccount | null {
  if (loaded) return serviceAccount;
  loaded = true;

  let raw = process.env.FCM_SERVICE_ACCOUNT?.trim();
  if (!raw) {
    const path = process.env.FCM_SERVICE_ACCOUNT_PATH?.trim();
    if (path) {
      try {
        raw = require("fs").readFileSync(path, "utf-8");
      } catch (e) {
        console.warn("[fcm] Ne mogu da pročitam FCM_SERVICE_ACCOUNT_PATH:", e);
        return null;
      }
    }
  }
  if (!raw) return null;

  try {
    const parsed = JSON.parse(raw) as ServiceAccount;
    if (!parsed.client_email || !parsed.private_key) {
      console.warn("[fcm] Service account nema client_email/private_key.");
      return null;
    }
    parsed.project_id = process.env.FCM_PROJECT_ID?.trim() || parsed.project_id;
    serviceAccount = parsed;
    return serviceAccount;
  } catch (e) {
    console.warn("[fcm] Neispravan FCM_SERVICE_ACCOUNT JSON:", e);
    return null;
  }
}

export function isFcmConfigured(): boolean {
  return loadServiceAccount() !== null;
}

// Keš OAuth2 access tokena (važi ~1h); osvežava se malo pre isteka.
let cachedAccessToken: { token: string; expiresAt: number } | null = null;

async function getAccessToken(sa: ServiceAccount): Promise<string | null> {
  const now = Math.floor(Date.now() / 1000);
  if (cachedAccessToken && cachedAccessToken.expiresAt - 60 > now) {
    return cachedAccessToken.token;
  }

  try {
    const key = await importPKCS8(sa.private_key, "RS256");
    const assertion = await new SignJWT({ scope: SCOPE })
      .setProtectedHeader({ alg: "RS256" })
      .setIssuer(sa.client_email)
      .setSubject(sa.client_email)
      .setAudience(TOKEN_ENDPOINT)
      .setIssuedAt(now)
      .setExpirationTime(now + 3600)
      .sign(key);

    const res = await fetch(TOKEN_ENDPOINT, {
      method: "POST",
      headers: { "Content-Type": "application/x-www-form-urlencoded" },
      body: new URLSearchParams({
        grant_type: "urn:ietf:params:oauth:grant-type:jwt-bearer",
        assertion,
      }),
    });

    if (!res.ok) {
      console.warn("[fcm] OAuth2 greška:", res.status, await res.text());
      return null;
    }

    const data = (await res.json()) as { access_token: string; expires_in: number };
    cachedAccessToken = {
      token: data.access_token,
      expiresAt: now + (data.expires_in ?? 3600),
    };
    return cachedAccessToken.token;
  } catch (e) {
    console.warn("[fcm] Ne mogu da pribavim access token:", e);
    return null;
  }
}

function getUserTokens(userId: number): string[] {
  const rows = db
    .query("SELECT token FROM fcm_tokens WHERE user_id = ?")
    .all(userId) as Array<{ token: string }>;
  return rows.map((r) => r.token);
}

function deleteToken(token: string): void {
  db.query("DELETE FROM fcm_tokens WHERE token = ?").run(token);
}

/**
 * Šalje data-only push notifikaciju na sve uređaje korisnika. Aplikacija sama gradi
 * notifikaciju na tačnom kanalu iz `data` polja. No-op ako FCM nije konfigurisan ili
 * korisnik nema registrovanih tokena. Fire-and-forget — pozivaoci ne moraju await.
 */
export async function sendFcmToUser(
  userId: number,
  category: string,
  title: string,
  body: string,
  extra: Record<string, string> = {},
): Promise<void> {
  const sa = loadServiceAccount();
  if (!sa) return;

  const tokens = getUserTokens(userId);
  if (tokens.length === 0) return;

  const accessToken = await getAccessToken(sa);
  if (!accessToken) return;

  const url = `https://fcm.googleapis.com/v1/projects/${sa.project_id}/messages:send`;

  await Promise.all(
    tokens.map(async (token) => {
      const message = {
        message: {
          token,
          data: { category, title, body, ...extra },
          android: { priority: "high" as const },
        },
      };
      try {
        const res = await fetch(url, {
          method: "POST",
          headers: {
            Authorization: `Bearer ${accessToken}`,
            "Content-Type": "application/json",
          },
          body: JSON.stringify(message),
        });
        // Neispravan/istekao token — očisti ga da ne šaljemo ponovo.
        if (res.status === 404 || res.status === 400) {
          const errText = await res.text();
          if (errText.includes("UNREGISTERED") || errText.includes("INVALID_ARGUMENT")) {
            deleteToken(token);
          } else {
            console.warn("[fcm] Slanje nije uspelo:", res.status, errText);
          }
        } else if (!res.ok) {
          console.warn("[fcm] Slanje nije uspelo:", res.status, await res.text());
        }
      } catch (e) {
        console.warn("[fcm] Mrežna greška pri slanju:", e);
      }
    }),
  );
}
