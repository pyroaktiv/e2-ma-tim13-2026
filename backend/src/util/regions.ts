interface Region {
  id: number;
  name: string;
  icon: string;
  latMin: number;
  latMax: number;
  lngMin: number;
  lngMax: number;
}

export const REGIONS: Region[] = [
  {
    id: 1,
    name: "Vojvodina",
    icon: "region_vojvodina",
    latMin: 44.57,
    latMax: 46.18,
    lngMin: 18.83,
    lngMax: 21.60,
  },
  {
    id: 2,
    name: "Beograd",
    icon: "region_beograd",
    latMin: 44.28,
    latMax: 44.98,
    lngMin: 20.04,
    lngMax: 20.92,
  },
  {
    id: 3,
    name: "Šumadija i Zapadna Srbija",
    icon: "region_sumadija",
    latMin: 43.35,
    latMax: 44.72,
    lngMin: 19.18,
    lngMax: 21.56,
  },
  {
    id: 4,
    name: "Južna i Istočna Srbija",
    icon: "region_jug",
    latMin: 42.24,
    latMax: 44.05,
    lngMin: 20.60,
    lngMax: 22.93,
  },
];

export const REGION_NAMES = REGIONS.map((r) => r.name) as [string, ...string[]];

export function generateRandomPoint(regionName: string): { lat: number; lng: number } {
  const region = REGIONS.find((r) => r.name === regionName);
  if (!region) throw new Error(`Unknown region: ${regionName}`);
  const lat = region.latMin + Math.random() * (region.latMax - region.latMin);
  const lng = region.lngMin + Math.random() * (region.lngMax - region.lngMin);
  return {
    lat: Math.round(lat * 1e6) / 1e6,
    lng: Math.round(lng * 1e6) / 1e6,
  };
}
