import { db } from "../db/database";

/**
 * Sastavlja sadržaj jedne partije: za četiri igre čita kurirani pool iz `game_content` i bira
 * potreban broj stavki; skočko i moj broj generiše nasumično. Ključevi su camelCase, kako ih
 * klijent očekuje (podrazumevani Gson preko socketa).
 */
export function buildMatchContent(): Record<string, unknown> {
  return {
    koZnaZna: pickRandom(pool("ko_zna_zna"), 5),
    spojnice: pickRandom(pool("spojnice"), 2),
    asocijacije: pickRandom(pool("asocijacije"), 2),
    skocko: [randomSecret(), randomSecret()],
    mojBroj: [randomMojBroj(), randomMojBroj()],
    korakPoKorak: pickRandom(pool("korak_po_korak"), 2),
  };
}

function pool(game: string): unknown[] {
  const row = db
    .query("SELECT content_json FROM game_content WHERE game = ?")
    .get(game) as { content_json: string } | null;
  if (!row) return [];
  try {
    const parsed = JSON.parse(row.content_json);
    return Array.isArray(parsed) ? parsed : [];
  } catch {
    return [];
  }
}

function pickRandom<T>(items: T[], count: number): T[] {
  const shuffled = [...items];
  for (let i = shuffled.length - 1; i > 0; i--) {
    const j = Math.floor(Math.random() * (i + 1));
    [shuffled[i], shuffled[j]] = [shuffled[j]!, shuffled[i]!];
  }
  return shuffled.slice(0, Math.min(count, shuffled.length));
}

const SKOCKO_SYMBOLS = ["SKOCKO", "KVADRAT", "KRUG", "SRCE", "TROUGAO", "ZVEZDA"];

function randomSecret(): string[] {
  return Array.from({ length: 4 }, () => SKOCKO_SYMBOLS[Math.floor(Math.random() * SKOCKO_SYMBOLS.length)]!);
}

function randomMojBroj(): { target: number; numbers: number[] } {
  const numbers = [
    ...Array.from({ length: 4 }, () => 1 + Math.floor(Math.random() * 9)),
    [10, 15, 20][Math.floor(Math.random() * 3)]!,
    [25, 50, 75, 100][Math.floor(Math.random() * 4)]!,
  ];
  return { target: 100 + Math.floor(Math.random() * 900), numbers };
}
