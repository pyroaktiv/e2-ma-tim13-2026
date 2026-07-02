import { db } from "../../db/database";
import { requireAuth } from "../../middleware/auth";
import { json } from "../../util/response";
import { REGIONS, REGION_NAMES, generateRandomPoint, pointInSerbia } from "../../util/regions";
import { isOnline } from "../../util/websocket";

export function handleGetRegionList(_req: Request): Response {
  return json(REGIONS.map((r) => ({ name: r.name, icon: r.icon })));
}

export async function handleGetRegions(req: Request): Promise<Response> {
  const auth = await requireAuth(req);
  if (auth instanceof Response) return auth;

  const userRegion = db
    .query("SELECT region FROM users WHERE id = ?")
    .get(auth.user_id) as { region: string } | null;

  const rows = REGIONS.map((region) => {
    const stats = db
      .query(
        "SELECT COALESCE(SUM(monthly_stars), 0) AS monthly_stars, COUNT(*) AS total_players FROM users WHERE region = ?",
      )
      .get(region.name) as { monthly_stars: number; total_players: number };
    return { name: region.name, icon: region.icon, ...stats };
  });

  rows.sort((a, b) => b.monthly_stars - a.monthly_stars);

  return json(
    rows.map((row, index) => ({
      name: row.name,
      icon: row.icon,
      monthly_stars: row.monthly_stars,
      rank: index + 1,
      total_players: row.total_players,
      is_own_region: row.name === userRegion?.region,
    })),
  );
}

export async function handleGetRegionMap(req: Request): Promise<Response> {
  const auth = await requireAuth(req);
  if (auth instanceof Response) return auth;

  // Popuni/ispravi tačke van Srbije (spec 5.a: čiode isključivo na teritoriji Srbije).
  const all = db
    .query("SELECT id, region, map_lat, map_lng FROM users")
    .all() as Array<{ id: number; region: string; map_lat: number | null; map_lng: number | null }>;

  const upd = db.prepare("UPDATE users SET map_lat = ?, map_lng = ? WHERE id = ?");
  const heal = db.transaction(() => {
    for (const u of all) {
      if (!REGION_NAMES.includes(u.region)) continue; // nepoznat region -> preskoči
      if (u.map_lat == null || u.map_lng == null || !pointInSerbia(u.map_lat, u.map_lng)) {
        const p = generateRandomPoint(u.region);
        upd.run(p.lat, p.lng, u.id);
      }
    }
  });
  heal();

  const rows = db
    .query(
      "SELECT region, map_lat AS lat, map_lng AS lng FROM users WHERE map_lat IS NOT NULL",
    )
    .all() as Array<{ region: string; lat: number; lng: number }>;

  return json(rows);
}

export async function handleGetRegionStats(req: Request): Promise<Response> {
  const auth = await requireAuth(req);
  if (auth instanceof Response) return auth;

  const url = new URL(req.url);
  const regionName = decodeURIComponent(url.pathname.split("/")[3] ?? "");

  const regionMeta = REGIONS.find((r) => r.name === regionName);
  if (!regionMeta) return json({ error: "Region not found" }, 404);

  const placeCounts = db
    .query(
      `SELECT
         COUNT(CASE WHEN rank = 1 THEN 1 END) AS first_place_count,
         COUNT(CASE WHEN rank = 2 THEN 1 END) AS second_place_count,
         COUNT(CASE WHEN rank = 3 THEN 1 END) AS third_place_count
       FROM monthly_region_stats
       WHERE region_name = ?`,
    )
    .get(regionName) as {
    first_place_count: number;
    second_place_count: number;
    third_place_count: number;
  };

  const totals = db
    .query(
      `SELECT COUNT(*) AS total_players, COALESCE(SUM(monthly_stars), 0) AS current_monthly_stars
       FROM users WHERE region = ?`,
    )
    .get(regionName) as { total_players: number; current_monthly_stars: number };

  const regionUserIds = db
    .query("SELECT id FROM users WHERE region = ?")
    .all(regionName) as Array<{ id: number }>;
  const activePlayers = regionUserIds.filter((u) => isOnline(u.id)).length;

  return json({
    name: regionName,
    icon: regionMeta.icon,
    first_place_count: placeCounts.first_place_count,
    second_place_count: placeCounts.second_place_count,
    third_place_count: placeCounts.third_place_count,
    active_players: activePlayers,
    total_players: totals.total_players,
    current_monthly_stars: totals.current_monthly_stars,
  });
}
