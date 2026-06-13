// Static game content for the real-time games owned by Student 2.
// Per the project notes it is enough to have a handful of examples so a match
// can be played. The pools are shuffled and sliced when a match starts.

export interface KoZnaZnaQuestion {
  text: string;
  answers: [string, string, string, string];
  correct: number; // index 0..3
}

export interface SpojniceRound {
  criterion: string; // e.g. "Poveži glumca sa likom koji tumači"
  left: string[]; // 5 prompts
  right: string[]; // 5 prompts (shuffled display order)
  // solution[i] = index in `right` that matches left[i]
  solution: number[];
}

export const KO_ZNA_ZNA_POOL: KoZnaZnaQuestion[] = [
  {
    text: "Koji je glavni grad Australije?",
    answers: ["Sidnej", "Melburn", "Kanbera", "Pert"],
    correct: 2,
  },
  {
    text: "Koja planeta je najbliža Suncu?",
    answers: ["Venera", "Zemlja", "Mars", "Merkur"],
    correct: 3,
  },
  {
    text: "Ko je napisao roman 'Na Drini ćuprija'?",
    answers: ["Miloš Crnjanski", "Ivo Andrić", "Meša Selimović", "Dobrica Ćosić"],
    correct: 1,
  },
  {
    text: "Koja je najduža reka u Evropi?",
    answers: ["Dunav", "Volga", "Sava", "Rajna"],
    correct: 1,
  },
  {
    text: "Koliko kontinenata postoji na Zemlji?",
    answers: ["5", "6", "7", "8"],
    correct: 2,
  },
  {
    text: "Koje godine je počeo Prvi svetski rat?",
    answers: ["1912", "1914", "1918", "1939"],
    correct: 1,
  },
  {
    text: "Koji hemijski element ima oznaku 'O'?",
    answers: ["Zlato", "Osmijum", "Kiseonik", "Olovo"],
    correct: 2,
  },
  {
    text: "Ko je naslikao Mona Lizu?",
    answers: ["Mikelanđelo", "Leonardo da Vinči", "Rafael", "Donatelo"],
    correct: 1,
  },
  {
    text: "Koja je najveća planeta Sunčevog sistema?",
    answers: ["Saturn", "Neptun", "Jupiter", "Uran"],
    correct: 2,
  },
  {
    text: "Glavni grad Japana je?",
    answers: ["Osaka", "Kjoto", "Tokio", "Nagoja"],
    correct: 2,
  },
  {
    text: "Koliko strana ima šestougao?",
    answers: ["5", "6", "7", "8"],
    correct: 1,
  },
  {
    text: "Koji je najveći okean na svetu?",
    answers: ["Atlantski", "Indijski", "Tihi", "Severni ledeni"],
    correct: 2,
  },
  {
    text: "Ko je režirao film 'Ko to tamo peva'?",
    answers: ["Emir Kusturica", "Slobodan Šijan", "Goran Paskaljević", "Srđan Dragojević"],
    correct: 1,
  },
  {
    text: "Koliko žica ima klasična gitara?",
    answers: ["4", "5", "6", "7"],
    correct: 2,
  },
  {
    text: "U kojoj državi se nalazi grad Marakeš?",
    answers: ["Egipat", "Maroko", "Tunis", "Alžir"],
    correct: 1,
  },
  {
    text: "Koji metal je tečan na sobnoj temperaturi?",
    answers: ["Živa", "Olovo", "Kalaj", "Cink"],
    correct: 0,
  },
  {
    text: "Koje godine je čovek prvi put kročio na Mesec?",
    answers: ["1965", "1969", "1972", "1958"],
    correct: 1,
  },
  {
    text: "Ko je komponovao 'Za Elizu'?",
    answers: ["Mocart", "Betoven", "Bah", "Šopen"],
    correct: 1,
  },
];

export const SPOJNICE_POOL: SpojniceRound[] = [
  {
    criterion: "Poveži državu sa njenim glavnim gradom",
    left: ["Francuska", "Italija", "Španija", "Nemačka", "Grčka"],
    right: ["Madrid", "Atina", "Pariz", "Berlin", "Rim"],
    solution: [2, 4, 0, 3, 1],
  },
  {
    criterion: "Poveži pisca sa njegovim delom",
    left: ["Ivo Andrić", "Meša Selimović", "Branko Ćopić", "Petar Kočić", "Jovan Dučić"],
    right: ["Jazavac pred sudom", "Derviš i smrt", "Na Drini ćuprija", "Pjesme", "Orlovi rano lete"],
    solution: [2, 1, 4, 0, 3],
  },
  {
    criterion: "Poveži životinju sa grupom kojoj pripada",
    left: ["Delfin", "Orao", "Žaba", "Ajkula", "Zmija"],
    right: ["Ptica", "Riba", "Vodozemac", "Sisar", "Gmizavac"],
    solution: [3, 0, 2, 1, 4],
  },
  {
    criterion: "Poveži sportistu sa sportom",
    left: ["Novak Đoković", "Nikola Jokić", "Lionel Mesi", "Lebron Džejms", "Tajger Vuds"],
    right: ["Košarka", "Fudbal", "Tenis", "Golf", "NBA"],
    solution: [2, 0, 1, 4, 3],
  },
  {
    criterion: "Poveži planetu sa rednim brojem od Sunca",
    left: ["Merkur", "Zemlja", "Mars", "Jupiter", "Saturn"],
    right: ["Treća", "Šesta", "Prva", "Peta", "Četvrta"],
    solution: [2, 0, 4, 3, 1],
  },
  {
    criterion: "Poveži izum sa pronalazačem",
    left: ["Sijalica", "Telefon", "Radio", "Dinamit", "Točak"],
    right: ["Tesla", "Nobel", "Edison", "Praistorija", "Bel"],
    solution: [2, 4, 0, 1, 3],
  },
  {
    criterion: "Poveži reku sa kontinentom",
    left: ["Amazon", "Nil", "Dunav", "Misisipi", "Gang"],
    right: ["Evropa", "Azija", "Južna Amerika", "Severna Amerika", "Afrika"],
    solution: [2, 4, 0, 3, 1],
  },
];

// ---- Skočko ----------------------------------------------------------------
export const SKOCKO_SYMBOLS = 6; // skočko, kvadrat, krug, srce, trougao, zvezda
export const SKOCKO_LENGTH = 4;

// ---- Korak po korak --------------------------------------------------------
export interface KorakPoKorakRound {
  answer: string;
  accepted: string[]; // normalized accepted answers
  clues: string[]; // 7 clues, hardest first
}

export const KORAK_POOL: KorakPoKorakRound[] = [
  {
    answer: "Beograd",
    accepted: ["beograd"],
    clues: [
      "Ime mu je slovenskog porekla",
      "Nalazi se na ušću dve reke",
      "Kalemegdan",
      "Ima preko milion stanovnika",
      "Najveći grad u svojoj državi",
      "Sava se tu uliva u Dunav",
      "Glavni grad Srbije",
    ],
  },
  {
    answer: "Sunce",
    accepted: ["sunce"],
    clues: [
      "Staro je oko 4,6 milijardi godina",
      "Sastoji se uglavnom od vodonika i helijuma",
      "Udaljeno oko 150 miliona km od nas",
      "Oko njega kruže planete",
      "Izvor svetlosti i toplote",
      "Izlazi na istoku",
      "Zvezda u centru našeg sistema",
    ],
  },
  {
    answer: "Tesla",
    accepted: ["tesla", "nikola tesla"],
    clues: [
      "Rođen je 1856. godine",
      "Bio je inženjer i pronalazač",
      "Radio je neko vreme kod Edisona",
      "Po njemu je nazvana jedinica za magnetnu indukciju",
      "Zaslužan za naizmeničnu struju",
      "Srpski naučnik svetskog glasa",
      "Prezime mu nosi i poznata auto-kompanija",
    ],
  },
  {
    answer: "Dunav",
    accepted: ["dunav"],
    clues: [
      "Druga je po dužini na svom kontinentu",
      "Izvire u planini Švarcvald",
      "Prolazi kroz deset država",
      "Uliva se u Crno more",
      "Na njemu leže Beč i Budimpešta",
      "Kroz Srbiju protiče sa severa",
      "U Beogradu se u njega uliva Sava",
    ],
  },
  {
    answer: "Šah",
    accepted: ["sah", "šah"],
    clues: [
      "Potiče iz Indije",
      "Igra se već vekovima",
      "Postoji svetski šampionat",
      "Tabla ima 64 polja",
      "Igra za dva igrača",
      "Cilj je matirati protivnika",
      "Kralj, kraljica, top, lovac, skakač i pešak",
    ],
  },
];

// ---- Asocijacije -----------------------------------------------------------
export interface AsocijacijeColumn {
  solution: string;
  accepted: string[];
  fields: [string, string, string, string];
}
export interface AsocijacijeRound {
  columns: [AsocijacijeColumn, AsocijacijeColumn, AsocijacijeColumn, AsocijacijeColumn];
  finalSolution: string;
  finalAccepted: string[];
}

export const ASOCIJACIJE_POOL: AsocijacijeRound[] = [
  {
    columns: [
      { solution: "KVIZ", accepted: ["kviz"], fields: ["Pitanje", "Odgovor", "Takmičenje", "Znanje"] },
      { solution: "TELEVIZIJA", accepted: ["televizija", "tv"], fields: ["Ekran", "Program", "Daljinski", "Kanal"] },
      { solution: "IGRA", accepted: ["igra"], fields: ["Zabava", "Pravila", "Pobednik", "Protivnik"] },
      { solution: "REČI", accepted: ["reči", "reci"], fields: ["Slovo", "Rečnik", "Govor", "Jezik"] },
    ],
    finalSolution: "SLAGALICA",
    finalAccepted: ["slagalica"],
  },
  {
    columns: [
      { solution: "LOPTA", accepted: ["lopta"], fields: ["Okrugla", "Šut", "Gol", "Teren"] },
      { solution: "TIM", accepted: ["tim", "ekipa"], fields: ["Saigrač", "Dres", "Kapiten", "Klupa"] },
      { solution: "SUDIJA", accepted: ["sudija"], fields: ["Pištaljka", "Karton", "Faul", "Prekršaj"] },
      { solution: "NAVIJAČI", accepted: ["navijači", "navijaci"], fields: ["Tribina", "Zastava", "Bakljada", "Skandiranje"] },
    ],
    finalSolution: "FUDBAL",
    finalAccepted: ["fudbal"],
  },
  {
    columns: [
      { solution: "MORE", accepted: ["more"], fields: ["Talas", "So", "Plaža", "Brod"] },
      { solution: "SUNCE", accepted: ["sunce"], fields: ["Zraci", "Toplota", "Leto", "Žuto"] },
      { solution: "PESAK", accepted: ["pesak"], fields: ["Plaža", "Sat", "Pustinja", "Zrno"] },
      { solution: "ODMOR", accepted: ["odmor"], fields: ["Godišnji", "Opuštanje", "Putovanje", "Hotel"] },
    ],
    finalSolution: "LETOVANJE",
    finalAccepted: ["letovanje", "leto"],
  },
  {
    columns: [
      { solution: "SNEG", accepted: ["sneg"], fields: ["Belo", "Pahulja", "Hladno", "Grudva"] },
      { solution: "JELKA", accepted: ["jelka"], fields: ["Iglice", "Ukrasi", "Lampice", "Zvezda"] },
      { solution: "POKLON", accepted: ["poklon"], fields: ["Kutija", "Mašna", "Iznenađenje", "Deda Mraz"] },
      { solution: "PRAZNIK", accepted: ["praznik"], fields: ["Slava", "Trpeza", "Porodica", "Neradni"] },
    ],
    finalSolution: "NOVA GODINA",
    finalAccepted: ["nova godina", "novagodina"],
  },
];

export function shuffle<T>(arr: readonly T[]): T[] {
  const copy = [...arr];
  for (let i = copy.length - 1; i > 0; i--) {
    const j = Math.floor(Math.random() * (i + 1));
    [copy[i], copy[j]] = [copy[j]!, copy[i]!];
  }
  return copy;
}
