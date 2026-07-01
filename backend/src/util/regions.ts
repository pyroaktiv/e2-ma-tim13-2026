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

// Grub poligon granice Srbije ([lat, lng]) — tačke van njega se odbacuju (spec 5.a: čiode su na teritoriji Srbije).
const SERBIA: Array<[number, number]> = [
  [46.18, 19.65], [46.1, 20.3], [45.78, 20.8], [44.7, 22.55],
  [44.05, 22.7], [43.2, 22.95], [42.95, 22.45], [42.35, 22.1],
  [42.25, 21.55], [42.2, 20.55], [42.55, 20.05], [43.25, 19.2],
  [43.55, 19.1], [44.3, 19.1], [44.9, 19.0], [45.1, 19.3],
  [45.2, 18.9], [45.55, 18.85], [45.9, 19.1],
];

/** Ray-casting: da li je tačka (lat,lng) unutar poligona Srbije. */
export function pointInSerbia(lat: number, lng: number): boolean {
  let inside = false;
  for (let i = 0, j = SERBIA.length - 1; i < SERBIA.length; j = i++) {
    const yi = SERBIA[i]![0];
    const xi = SERBIA[i]![1];
    const yj = SERBIA[j]![0];
    const xj = SERBIA[j]![1];
    const intersect = yi > lat !== yj > lat && lng < ((xj - xi) * (lat - yi)) / (yj - yi) + xi;
    if (intersect) inside = !inside;
  }
  return inside;
}

const round5 = (n: number): number => Math.round(n * 1e5) / 1e5;

export function generateRandomPoint(regionName: string): { lat: number; lng: number } {
  const region = REGIONS.find((r) => r.name === regionName);
  if (!region) throw new Error(`Unknown region: ${regionName}`);
  // Odbacuj tačke van poligona Srbije; pokušaj do 80 puta, pa fallback na centar okvira (u Srbiji).
  for (let i = 0; i < 80; i++) {
    const lat = region.latMin + Math.random() * (region.latMax - region.latMin);
    const lng = region.lngMin + Math.random() * (region.lngMax - region.lngMin);
    if (pointInSerbia(lat, lng)) return { lat: round5(lat), lng: round5(lng) };
  }
  return {
    lat: round5((region.latMin + region.latMax) / 2),
    lng: round5((region.lngMin + region.lngMax) / 2),
  };
}
