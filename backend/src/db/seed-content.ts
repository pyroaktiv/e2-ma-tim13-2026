import { db } from "./database";

/**
 * Seeduje sadržaj igara (spec napomena: „podatke ... uneti u bazu") za četiri igre čiji je
 * sadržaj kuriran: ko zna zna, spojnice, asocijacije, korak po korak. Skočko i moj broj se
 * generišu nasumično u runtime-u (vidi src/match/content.ts).
 *
 * Svaki red je „pool" iz koga se za partiju bira potreban broj stavki (ključevi su camelCase
 * jer ih klijent čita podrazumevanim Gson-om preko socketa).
 */
export function seedGameContent(): void {
  const upsert = db.prepare(
    "INSERT INTO game_content (game, content_json) VALUES (?, ?) ON CONFLICT(game) DO NOTHING",
  );

  upsert.run("ko_zna_zna", JSON.stringify(KO_ZNA_ZNA));
  upsert.run("spojnice", JSON.stringify(SPOJNICE));
  upsert.run("asocijacije", JSON.stringify(ASOCIJACIJE));
  upsert.run("korak_po_korak", JSON.stringify(KORAK_PO_KORAK));
}

const KO_ZNA_ZNA = [
  { text: "Koji glumac tumači lik Popa u filmu 'Munje!'?", options: ["Nikola Đuričko", "Sergej Trifunović", "Boris Milivojević", "Nenad Jezdić"], correctIndex: 1 },
  { text: "Koji je glavni grad Australije?", options: ["Sidnej", "Melburn", "Kanbera", "Pert"], correctIndex: 2 },
  { text: "Koja planeta je najbliža Suncu?", options: ["Venera", "Zemlja", "Mars", "Merkur"], correctIndex: 3 },
  { text: "Ko je napisao roman 'Na Drini ćuprija'?", options: ["Miloš Crnjanski", "Ivo Andrić", "Meša Selimović", "Dobrica Ćosić"], correctIndex: 1 },
  { text: "Koja je najduža reka u Evropi?", options: ["Dunav", "Volga", "Sava", "Rajna"], correctIndex: 1 },
  { text: "Koje godine je počeo Prvi svetski rat?", options: ["1912", "1914", "1918", "1939"], correctIndex: 1 },
  { text: "Koliko kontinenata ima na Zemlji?", options: ["5", "6", "7", "8"], correctIndex: 2 },
  { text: "Koji je hemijski simbol za zlato?", options: ["Zl", "Au", "Ag", "Go"], correctIndex: 1 },
  { text: "Ko je naslikao 'Mona Lizu'?", options: ["Mikelanđelo", "Leonardo da Vinči", "Rafael", "Pikaso"], correctIndex: 1 },
  { text: "Koji je najviši vrh na svetu?", options: ["K2", "Mont Blan", "Everest", "Kilimandžaro"], correctIndex: 2 },
];

const SPOJNICE = [
  {
    leftItems: ["Pop", "Mare", "Gojko Šiša", "Kata", "Policajac"],
    rightItems: ["Sergej Trifunović", "Nebojša Glogovac", "Nikola Đuričko", "Maja Mandžuka", "Boris Milivojević"],
    solution: [0, 4, 2, 3, 1],
  },
  {
    leftItems: ["Kengur", "Braca", "Šomi", "Iris", "Živac"],
    rightItems: ["Marija Karan", "Nebojša Glogovac", "Boris Milivojević", "Nikola Đuričko", "Sergej Trifunović"],
    solution: [3, 4, 2, 0, 1],
  },
  {
    leftItems: ["Srbija", "Italija", "Nemačka", "Francuska", "Španija"],
    rightItems: ["Madrid", "Beograd", "Pariz", "Rim", "Berlin"],
    solution: [1, 3, 4, 2, 0],
  },
  {
    leftItems: ["Pas", "Mačka", "Konj", "Krava", "Ovca"],
    rightItems: ["Mjau", "Av", "Mu", "Be", "Iha"],
    solution: [1, 0, 4, 2, 3],
  },
];

const ASOCIJACIJE = [
  {
    columns: [
      { index: 0, fields: ["NIL", "AMAZON", "DUNAV", "VOLGA"], solution: "REKE" },
      { index: 1, fields: ["TIGAR", "LAV", "GEPARD", "LEOPARD"], solution: "DIVLJE MAČKE" },
      { index: 2, fields: ["EVEREST", "MONT BLAN", "OLIMP", "FUDŽIJAMA"], solution: "PLANINE" },
      { index: 3, fields: ["PERO", "HEMIJSKA", "MARKER", "FLOMASTER"], solution: "PISANJE" },
    ],
    finalSolution: "GEOGRAFIJA",
  },
  {
    columns: [
      { index: 0, fields: ["JABUKA", "KRUŠKA", "ŠLJIVA", "TREŠNJA"], solution: "VOĆE" },
      { index: 1, fields: ["CRVENA", "ŽUTA", "PLAVA", "ZELENA"], solution: "BOJE" },
      { index: 2, fields: ["PAS", "MAČKA", "ZEC", "HRČAK"], solution: "KUĆNI LJUBIMCI" },
      { index: 3, fields: ["FUDBAL", "KOŠARKA", "TENIS", "ODBOJKA"], solution: "SPORTOVI" },
    ],
    finalSolution: "SLOBODNO VREME",
  },
  {
    columns: [
      { index: 0, fields: ["SUNCE", "MESEC", "ZVEZDA", "KOMETA"], solution: "NEBO" },
      { index: 1, fields: ["ZIMA", "PROLEĆE", "LETO", "JESEN"], solution: "GODIŠNJA DOBA" },
      { index: 2, fields: ["GITARA", "KLAVIR", "VIOLINA", "BUBANJ"], solution: "INSTRUMENTI" },
      { index: 3, fields: ["LAV", "BIK", "RAK", "RIBE"], solution: "HOROSKOP" },
    ],
    finalSolution: "PRIRODA I VREME",
  },
];

const KORAK_PO_KORAK = [
  {
    clues: [
      "Može biti bela, crna ili mlečna",
      "Često se poklanja za praznike",
      "Sadrži kakao i maslac",
      "Švajcarska je poznata po njoj",
      "Može biti sa lešnicima",
      "Topi se na suncu",
      "Slatkiš u kockicama",
    ],
    solution: "ČOKOLADA",
  },
  {
    clues: [
      "Najveća je mačka na svetu",
      "Ima prepoznatljive pruge",
      "Odličan je plivač",
      "Živi u azijskim džunglama",
      "Simbol je snage i moći",
      "U Sibiru dostiže najveću veličinu",
      "Šir Kan je jedan od njih",
    ],
    solution: "TIGAR",
  },
  {
    clues: [
      "Ima ih osam",
      "Pravi mrežu",
      "Nije insekt",
      "Neki su otrovni",
      "Lovi muve",
      "Spiderman je čovek-ovo",
      "Plaši mnoge ljude",
    ],
    solution: "PAUK",
  },
];
