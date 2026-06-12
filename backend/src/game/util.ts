// Small helpers shared by the text/number based games.

export function normalize(s: string): string {
  return s
    .toLowerCase()
    .normalize("NFD")
    .replace(/[̀-ͯ]/g, "") // strip diacritics
    .replace(/\s+/g, " ")
    .trim();
}

export function matchesAnswer(guess: string, accepted: string[]): boolean {
  const g = normalize(guess);
  return accepted.some((a) => normalize(a) === g);
}

/**
 * Safe evaluator for "Moj broj": parses an arithmetic expression over + - * /
 * and parentheses, and verifies that the integer literals used are a sub-multiset
 * of the provided numbers (each available number may be used at most once).
 * Returns the numeric result, or null if invalid.
 */
export function evalMojBroj(expr: string, available: number[]): number | null {
  const tokens = tokenize(expr);
  if (!tokens) return null;

  // verify number usage against the available multiset
  const pool = [...available];
  for (const t of tokens) {
    if (t.type === "num") {
      const idx = pool.indexOf(t.value);
      if (idx === -1) return null; // number not available (or used too many times)
      pool.splice(idx, 1);
    }
  }

  try {
    const parser = new Parser(tokens);
    const result = parser.parseExpression();
    if (!parser.atEnd()) return null;
    if (!Number.isFinite(result)) return null;
    return result;
  } catch {
    return null;
  }
}

type Token =
  | { type: "num"; value: number }
  | { type: "op"; value: "+" | "-" | "*" | "/" }
  | { type: "paren"; value: "(" | ")" };

function tokenize(expr: string): Token[] | null {
  const tokens: Token[] = [];
  let i = 0;
  while (i < expr.length) {
    const c = expr[i]!;
    if (c === " ") { i++; continue; }
    if (c >= "0" && c <= "9") {
      let num = "";
      while (i < expr.length && expr[i]! >= "0" && expr[i]! <= "9") { num += expr[i]; i++; }
      tokens.push({ type: "num", value: Number(num) });
      continue;
    }
    if (c === "+" || c === "-" || c === "*" || c === "/") {
      tokens.push({ type: "op", value: c });
      i++; continue;
    }
    if (c === "(" || c === ")") {
      tokens.push({ type: "paren", value: c });
      i++; continue;
    }
    return null; // illegal character
  }
  return tokens;
}

class Parser {
  private pos = 0;
  constructor(private readonly tokens: Token[]) {}

  atEnd(): boolean { return this.pos >= this.tokens.length; }
  private peek(): Token | undefined { return this.tokens[this.pos]; }

  parseExpression(): number {
    let value = this.parseTerm();
    while (true) {
      const t = this.peek();
      if (t?.type === "op" && (t.value === "+" || t.value === "-")) {
        this.pos++;
        const rhs = this.parseTerm();
        value = t.value === "+" ? value + rhs : value - rhs;
      } else break;
    }
    return value;
  }

  private parseTerm(): number {
    let value = this.parseFactor();
    while (true) {
      const t = this.peek();
      if (t?.type === "op" && (t.value === "*" || t.value === "/")) {
        this.pos++;
        const rhs = this.parseFactor();
        value = t.value === "*" ? value * rhs : value / rhs;
      } else break;
    }
    return value;
  }

  private parseFactor(): number {
    const t = this.peek();
    if (t?.type === "op" && t.value === "-") { this.pos++; return -this.parseFactor(); }
    if (t?.type === "op" && t.value === "+") { this.pos++; return this.parseFactor(); }
    if (t?.type === "paren" && t.value === "(") {
      this.pos++;
      const value = this.parseExpression();
      const close = this.peek();
      if (close?.type !== "paren" || close.value !== ")") throw new Error("expected )");
      this.pos++;
      return value;
    }
    if (t?.type === "num") { this.pos++; return t.value; }
    throw new Error("unexpected token");
  }
}

// Per-cell hints (Wordle style) used to color the Skočko grid:
// 2 = right symbol in the right place, 1 = symbol present elsewhere, 0 = absent.
export function skockoCellHints(secret: number[], guess: number[]): number[] {
  const hints = new Array(guess.length).fill(0);
  const remaining = new Map<number, number>();
  for (let i = 0; i < secret.length; i++) {
    if (guess[i] === secret[i]) hints[i] = 2;
    else remaining.set(secret[i]!, (remaining.get(secret[i]!) ?? 0) + 1);
  }
  for (let i = 0; i < guess.length; i++) {
    if (hints[i] === 2) continue;
    const left = remaining.get(guess[i]!) ?? 0;
    if (left > 0) {
      hints[i] = 1;
      remaining.set(guess[i]!, left - 1);
    }
  }
  return hints;
}

// Standard mastermind-style scoring used by Skočko.
export function skockoFeedback(secret: number[], guess: number[]): { exact: number; color: number } {
  let exact = 0;
  const secretCounts = new Map<number, number>();
  const guessCounts = new Map<number, number>();
  for (let i = 0; i < secret.length; i++) {
    if (guess[i] === secret[i]) {
      exact++;
    } else {
      secretCounts.set(secret[i]!, (secretCounts.get(secret[i]!) ?? 0) + 1);
      guessCounts.set(guess[i]!, (guessCounts.get(guess[i]!) ?? 0) + 1);
    }
  }
  let color = 0;
  for (const [sym, cnt] of guessCounts) {
    color += Math.min(cnt, secretCounts.get(sym) ?? 0);
  }
  return { exact, color };
}
