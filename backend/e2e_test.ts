// End-to-end smoke test: drives two players through a FULL 6-game partija.
// The bots know the static game data (Korak/Asocijacije answers) so the match
// finishes quickly instead of waiting out every timer.
import { KORAK_POOL, ASOCIJACIJE_POOL } from "./src/game/data";

const BASE = "http://localhost:3000";
const WS = "ws://localhost:3000/ws";

const KORAK_ANSWERS = KORAK_POOL.flatMap((r) => r.accepted);
function asoFinalByFieldText(text: string): string | undefined {
  for (const r of ASOCIJACIJE_POOL)
    for (const c of r.columns)
      if (c.fields.includes(text)) return r.finalAccepted[0];
  return undefined;
}

async function login(identifier: string, password: string): Promise<string> {
  const res = await fetch(`${BASE}/api/auth/login`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ identifier, password }),
  });
  return ((await res.json()) as { token: string }).token;
}

function makeClient(name: string, token: string, onOver: () => void): void {
  const ws = new WebSocket(`${WS}?token=${token}`);
  let slot = -1;
  const seenGames = new Set<string>();

  const send = (o: object) => ws.send(JSON.stringify(o));

  ws.onopen = () => send({ type: "quick_match" });
  ws.onmessage = (ev) => {
    const m = JSON.parse(ev.data as string);
    if (m.type === "match_found") slot = m.you;
    if (m.type === "game_intro" && !seenGames.has(m.game)) {
      seenGames.add(m.game);
      console.log(`[${name}] -> igra: ${m.game}`);
    }

    // Ko zna zna
    if (m.type === "kzz_question") {
      setTimeout(() => send({ type: "kzz_answer", answer: Math.floor(Math.random() * 4) }), 150);
    }
    // Spojnice
    if (m.type === "spj_round" || m.type === "spj_turn") {
      if (m.activePlayer === slot) {
        for (let i = 0; i < 5; i++) setTimeout(() => send({ type: "spojnice_connect", left: i, right: i }), 150 * (i + 1));
      }
    }
    // Asocijacije: open a field, then guess the (known) final
    if (m.type === "aso_round" || m.type === "aso_turn") {
      if (m.activePlayer === slot) setTimeout(() => send({ type: "aso_open", col: 0, field: 0 }), 120);
    }
    if (m.type === "aso_field" && m.by === slot) {
      const fin = asoFinalByFieldText(m.text);
      if (fin) setTimeout(() => send({ type: "aso_guess", target: "final", text: fin }), 120);
      else setTimeout(() => send({ type: "aso_pass" }), 120);
    }
    // Skočko: fire random guesses while it is our turn
    if (m.type === "skocko_round" && m.activePlayer === slot) {
      setTimeout(() => send({ type: "skocko_guess", guess: [0, 1, 2, 3] }), 120);
    }
    if (m.type === "skocko_feedback" && m.by === slot && !m.solved) {
      setTimeout(() => send({ type: "skocko_guess", guess: [Math.floor(Math.random() * 6), 1, 2, 3] }), 120);
    }
    // Korak po korak: wrong guesses don't end the turn, so spam known answers
    if ((m.type === "korak_round" || m.type === "korak_clue" || m.type === "korak_steal")) {
      const active = m.activePlayer !== undefined ? m.activePlayer === slot : true;
      if (active) KORAK_ANSWERS.forEach((a, i) => setTimeout(() => send({ type: "korak_guess", text: a }), 100 + i * 60));
    }
    // Moj broj: submit an expression using all six numbers
    if (m.type === "mojbroj_round") {
      const expr = (m.numbers as number[]).join("+");
      setTimeout(() => send({ type: "mojbroj_submit", expr }), 150);
    }

    if (m.type === "match_over") {
      console.log(`[${name}] MATCH OVER won=${m.youWon} tie=${m.tie} stars=${m.starsDelta} totals=${JSON.stringify(m.totalScores)}`);
      onOver();
    }
  };
  ws.onerror = (e: any) => console.log(`[${name}] error`, e?.message ?? e);
}

const peraToken = await login("pera", "Pera1234");
const mikaToken = await login("mika", "Mika1234");

let overCount = 0;
const done = new Promise<void>((resolve) => {
  const onOver = () => { if (++overCount >= 2) resolve(); };
  makeClient("pera", peraToken, onOver);
  setTimeout(() => makeClient("mika", mikaToken, onOver), 300);
});

await Promise.race([done, new Promise<void>((r) => setTimeout(r, 180000))]);
console.log("\n=== test finished ===");
process.exit(0);
