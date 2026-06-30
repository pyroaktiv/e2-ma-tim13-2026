import { db } from "../db/database";
import { requireAuth } from "../middleware/auth";
import { json } from "../util/response";
import { onlineUserIds } from "../util/websocket";
import {
  cycleRange,
  getPreviousRegionTop3,
  getRegionMedalCounts,
  getRegionMonthlyStars,
  monthlyCycleKey,
} from "../ranking/cycles";
import { pointInSerbia, randomPointIn, regionIcon, regionNames } from "../regions/config";

/**
 * Rute za prikaz regiona (spec 5): mesečni regionalni rang, mapa sa tačkama igrača i statistika
 * regiona. Mesečne zvezde se računaju po tekućem ciklusu (resetuju se svaki ciklus).
 */

/** Spec 5.b: mesečna rang lista po regionima + opseg ciklusa + top-3 prethodnog ciklusa. */
export async function handleGetRegionRanking(req: Request): Promise<Response> {
  const auth = await requireAuth(req);
  if (auth instanceof Response) return auth;

  const me = db.query("SELECT region FROM users WHERE id = ?").get(auth.user_id) as
    | { region: string }
    | null;

  const starsByRegion = new Map(getRegionMonthlyStars().map((r) => [r.region, r.stars]));
  const names = regionNames();

  const merged: Array<{ name: string; stars: number }> = names.map((name) => ({
    name,
    stars: starsByRegion.get(name) ?? 0,
  }));
  // Bezbednosno: i regioni iz baze koji nisu u konfiguraciji.
  for (const [name, stars] of starsByRegion) {
    if (!names.includes(name)) merged.push({ name, stars });
  }
  merged.sort((a, b) => b.stars - a.stars || a.name.localeCompare(b.name));

  const cycle = monthlyCycleKey();
  const range = cycleRange("monthly", cycle);

  return json({
    cycle,
    start: range.start,
    end: range.end,
    my_region: me?.region ?? null,
    previous_top3: getPreviousRegionTop3(),
    regions: merged.map((r, i) => ({
      region: r.name,
      icon: regionIcon(r.name),
      stars: r.stars,
      rank: i + 1,
      is_mine: r.name === me?.region,
    })),
  });
}

/** Spec 5.a: tačke svih igrača na mapi (nedostajuće se lenjo generišu). */
export async function handleGetRegionMap(req: Request): Promise<Response> {
  const auth = await requireAuth(req);
  if (auth instanceof Response) return auth;

  // Popuni nedostajuće I ispravi stare tačke koje su van Srbije (spec 5.a: čiode su na teritoriji Srbije).
  const all = db
    .query("SELECT id, region, map_lat, map_lng FROM users")
    .all() as Array<{ id: number; region: string; map_lat: number | null; map_lng: number | null }>;

  const upd = db.prepare("UPDATE users SET map_lat = ?, map_lng = ? WHERE id = ?");
  const tx = db.transaction(() => {
    for (const u of all) {
      if (u.map_lat == null || u.map_lng == null || !pointInSerbia(u.map_lat, u.map_lng)) {
        const p = randomPointIn(u.region);
        upd.run(p.lat, p.lng, u.id);
      }
    }
  });
  tx();

  const rows = db
    .query(
      `SELECT id, username, avatar, region, map_lat AS lat, map_lng AS lng
       FROM users WHERE map_lat IS NOT NULL AND map_lng IS NOT NULL`,
    )
    .all();

  return json(rows);
}

/** Spec 5.d: statistika regiona (medalje, aktivni i registrovani igrači). */
export async function handleGetRegionStats(req: Request): Promise<Response> {
  const auth = await requireAuth(req);
  if (auth instanceof Response) return auth;

  const region = new URL(req.url).searchParams.get("region");
  if (!region) return json({ error: "region is required" }, 400);

  const medals = getRegionMedalCounts()[region] ?? { first: 0, second: 0, third: 0 };

  const registered = (
    db.query("SELECT COUNT(*) AS c FROM users WHERE region = ?").get(region) as { c: number }
  ).c;

  const online = new Set(onlineUserIds());
  let active = 0;
  if (online.size > 0) {
    const inRegion = db.query("SELECT id FROM users WHERE region = ?").all(region) as Array<{ id: number }>;
    active = inRegion.filter((u) => online.has(u.id)).length;
  }

  return json({
    region,
    icon: regionIcon(region),
    first_places: medals.first,
    second_places: medals.second,
    third_places: medals.third,
    active_players: active,
    registered_players: registered,
  });
}
