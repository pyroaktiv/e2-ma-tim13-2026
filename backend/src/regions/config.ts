/**
 * Konfiguracija regiona (spec 5). Svaki region ima centar i „rasipanje" oko centra; nasumična
 * tačka igrača se generiše oko centra i ODBIJA dok ne padne unutar poligona Srbije, tako da su
 * čiode isključivo na teritoriji Srbije. Nazivi moraju da odgovaraju vrednostima koje klijent
 * šalje pri registraciji (res/values/strings.xml -> regioni_srbije).
 */
export interface RegionConfig {
  name: string;
  center: { lat: number; lng: number };
  spread: number;
}

export const REGIONS: RegionConfig[] = [
  { name: "Bačka", center: { lat: 45.55, lng: 19.45 }, spread: 0.22 },
  { name: "Banat", center: { lat: 45.4, lng: 20.7 }, spread: 0.22 },
  { name: "Srem", center: { lat: 45.0, lng: 19.65 }, spread: 0.18 },
  { name: "Beograd", center: { lat: 44.81, lng: 20.46 }, spread: 0.1 },
  { name: "Mačva", center: { lat: 44.75, lng: 19.55 }, spread: 0.12 },
  { name: "Šumadija", center: { lat: 44.05, lng: 20.78 }, spread: 0.2 },
  { name: "Pomoravlje", center: { lat: 43.95, lng: 21.25 }, spread: 0.15 },
  { name: "Braničevo", center: { lat: 44.5, lng: 21.3 }, spread: 0.18 },
  { name: "Zlatibor", center: { lat: 43.75, lng: 19.85 }, spread: 0.18 },
  { name: "Raška", center: { lat: 43.2, lng: 20.5 }, spread: 0.18 },
  { name: "Nišava", center: { lat: 43.3, lng: 22.0 }, spread: 0.16 },
  { name: "Toplica", center: { lat: 43.18, lng: 21.45 }, spread: 0.14 },
  { name: "Pčinja", center: { lat: 42.55, lng: 21.95 }, spread: 0.16 },
  { name: "Kosovo i Metohija", center: { lat: 42.66, lng: 20.9 }, spread: 0.3 },
];

const BY_NAME = new Map(REGIONS.map((r) => [r.name, r]));

// Grub poligon granice Srbije ([lat, lng]) — dovoljan da „zatvori" tačke unutar teritorije.
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
    const yi = SERBIA[i][0];
    const xi = SERBIA[i][1];
    const yj = SERBIA[j][0];
    const xj = SERBIA[j][1];
    const intersect =
      yi > lat !== yj > lat && lng < ((xj - xi) * (lat - yi)) / (yj - yi) + xi;
    if (intersect) inside = !inside;
  }
  return inside;
}

export function regionConfig(name: string): RegionConfig | undefined {
  return BY_NAME.get(name);
}

/** Sve igre koriste istu generičku čiodu; klijent je boji po nazivu regiona. */
export function regionIcon(_name: string): string {
  return "region_pin";
}

export function regionNames(): string[] {
  return REGIONS.map((r) => r.name);
}

const round5 = (n: number): number => Number(n.toFixed(5));

/** Nasumična tačka oko centra regiona, zagarantovano unutar poligona Srbije (spec 5.a). */
export function randomPointIn(name: string): { lat: number; lng: number } {
  const cfg = BY_NAME.get(name);
  const center = cfg ? cfg.center : { lat: 44.0, lng: 20.9 };
  const spread = cfg ? cfg.spread : 0.2;

  for (let i = 0; i < 60; i++) {
    const lat = center.lat + (Math.random() * 2 - 1) * spread;
    const lng = center.lng + (Math.random() * 2 - 1) * spread;
    if (pointInSerbia(lat, lng)) return { lat: round5(lat), lng: round5(lng) };
  }
  return { lat: round5(center.lat), lng: round5(center.lng) };
}
