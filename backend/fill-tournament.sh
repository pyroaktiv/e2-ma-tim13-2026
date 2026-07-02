#!/usr/bin/env bash
# fill-tournament.sh — queue 3 bot players so you can test the tournament UI with 1 emulator.
#
# Usage:
#   bash backend/fill-tournament.sh [BASE_URL]
#
# Examples:
#   bash backend/fill-tournament.sh                        # Android emulator default
#   bash backend/fill-tournament.sh http://localhost:3000  # host machine directly
#
# Prerequisites:
#   - Backend running (PORT=3000 bun index.ts in backend/)
#   - websocat installed (cargo install websocat  OR  brew install websocat)
#   - jq installed
#   - Log into the app as alice (Password1) before running this script

BASE="${1:-http://10.0.2.2:3000}"
WS_BASE="${BASE/http/ws}"

GREEN='\033[0;32m'; YELLOW='\033[1;33m'; RED='\033[0;31m'; NC='\033[0m'
info()    { echo -e "${YELLOW}[INFO]${NC} $*"; }
ok()      { echo -e "${GREEN}[ OK ]${NC} $*"; }
err_exit(){ echo -e "${RED}[ERR ]${NC} $*"; exit 1; }

# ── helpers ────────────────────────────────────────────────────────────────────

wait_for() {
  local file="$1" pattern="$2" secs="${3:-8}"
  local deadline=$(( $(date +%s) + secs ))
  while (( $(date +%s) < deadline )); do
    grep -q "$pattern" "$file" 2>/dev/null && return 0
    sleep 0.3
  done
  return 1
}

extract_field() {
  local file="$1" msg_type="$2" field="$3"
  grep -m1 "\"type\":\"$msg_type\"" "$file" 2>/dev/null | jq -r "$field" 2>/dev/null
}

post() {
  curl -s -X POST -H "Content-Type: application/json" -d "$2" "$BASE$1"
}

login() {
  post /api/auth/login "{\"identifier\":\"$1\",\"password\":\"Password1\"}" | jq -r '.token'
}

ws_send() { echo "$2" >> "$1"; }

# ── setup ─────────────────────────────────────────────────────────────────────

TMP=$(mktemp -d)
trap 'rm -rf "$TMP"; kill $(jobs -p) 2>/dev/null || true' EXIT

# Check for required tools
command -v websocat >/dev/null || err_exit "websocat not found. Install: cargo install websocat"
command -v jq       >/dev/null || err_exit "jq not found."

info "Server: $BASE"

# Restore test data so bots definitely have enough tokens (≥3 each)
curl -s -X POST "$BASE/api/test/restore-test-data" > /dev/null
ok "Test data restored (bots have fresh tokens)"

# Login 3 bots — use bob/user3/user4, leaving alice free for the human tester
T1=$(login bob);   [[ "$T1" == "null" || -z "$T1" ]] && err_exit "Login failed for bob. Is the server running?"
T2=$(login user3); [[ "$T2" == "null" || -z "$T2" ]] && err_exit "Login failed for user3."
T3=$(login user4); [[ "$T3" == "null" || -z "$T3" ]] && err_exit "Login failed for user4."
ok "Bots logged in (bob, user3, user4)"

# Setup per-bot files
for i in 1 2 3; do : > "$TMP/b${i}.out"; : > "$TMP/b${i}.in"; done

# Open WebSocket connections
tail -f "$TMP/b1.in" | stdbuf -oL websocat "$WS_BASE/ws?token=$T1" >> "$TMP/b1.out" 2>/dev/null &
tail -f "$TMP/b2.in" | stdbuf -oL websocat "$WS_BASE/ws?token=$T2" >> "$TMP/b2.out" 2>/dev/null &
tail -f "$TMP/b3.in" | stdbuf -oL websocat "$WS_BASE/ws?token=$T3" >> "$TMP/b3.out" 2>/dev/null &
sleep 0.6  # allow connections to establish

# ── queue all 3 bots ──────────────────────────────────────────────────────────

ws_send "$TMP/b1.in" '{"type":"find_tournament"}'
ws_send "$TMP/b2.in" '{"type":"find_tournament"}'
ws_send "$TMP/b3.in" '{"type":"find_tournament"}'

echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
ok "3 bots are in the tournament queue!"
echo "  → Open the app and tap 'Turnir' to join as the 4th player."
echo "  → Waiting up to 5 minutes for the tournament to start..."
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""

# ── wait for tournament_found on all 3 bots ───────────────────────────────────

for i in 1 2 3; do
  if ! wait_for "$TMP/b${i}.out" "tournament_found" 300; then
    err_exit "Bot $i didn't receive tournament_found within 5 minutes."
  fi
done
ok "Tournament started — all 3 bots received bracket assignments"

# ── determine which 2 bots are in the same semi (bot-vs-bot) ─────────────────

M1=$(extract_field "$TMP/b1.out" "tournament_found" ".matchId")
M2=$(extract_field "$TMP/b2.out" "tournament_found" ".matchId")
M3=$(extract_field "$TMP/b3.out" "tournament_found" ".matchId")
C1=$(extract_field "$TMP/b1.out" "tournament_found" ".color")
C2=$(extract_field "$TMP/b2.out" "tournament_found" ".color")

if [[ "$M1" == "$M2" ]]; then
  # Bot 1 (bob) vs Bot 2 (user3) — bot-vs-bot
  # Bot 3 (user4) is paired with the human → stays silent
  REPORTER_IN="$TMP/b1.in"; REPORTER_OUT="$TMP/b1.out"; REPORTER_COLOR="$C1"; REPORTER_MID="$M1"
  SILENT_SEMI_OUT="$TMP/b3.out"
  info "Bot semi: bob vs user3 | User's semi partner: user4 (silent)"
elif [[ "$M1" == "$M3" ]]; then
  # Bot 1 (bob) vs Bot 3 (user4) — bot-vs-bot
  # Bot 2 (user3) is paired with the human → stays silent
  REPORTER_IN="$TMP/b1.in"; REPORTER_OUT="$TMP/b1.out"; REPORTER_COLOR="$C1"; REPORTER_MID="$M1"
  SILENT_SEMI_OUT="$TMP/b2.out"
  info "Bot semi: bob vs user4 | User's semi partner: user3 (silent)"
else
  # Bot 2 (user3) vs Bot 3 (user4) — bot-vs-bot
  # Bot 1 (bob) is paired with the human → stays silent
  REPORTER_IN="$TMP/b2.in"; REPORTER_OUT="$TMP/b2.out"; REPORTER_COLOR="$C2"; REPORTER_MID="$M2"
  SILENT_SEMI_OUT="$TMP/b1.out"
  info "Bot semi: user3 vs user4 | User's semi partner: bob (silent)"
fi

# ── resolve bot-vs-bot semi (reporter bot wins) ───────────────────────────────

if [[ "$REPORTER_COLOR" == "BLUE" ]]; then
  ws_send "$REPORTER_IN" "{\"type\":\"report_result\",\"matchId\":\"$REPORTER_MID\",\"blueScore\":10,\"redScore\":3,\"perGame\":[]}"
  info "Bot semi reported: BLUE wins 10:3"
else
  ws_send "$REPORTER_IN" "{\"type\":\"report_result\",\"matchId\":\"$REPORTER_MID\",\"blueScore\":3,\"redScore\":10,\"perGame\":[]}"
  info "Bot semi reported: RED wins 3:10"
fi

if ! wait_for "$REPORTER_OUT" "tournament_semi_over" 15; then
  err_exit "Bot semi didn't resolve within 15s — server issue?"
fi
ok "Bot semi resolved"

# ── wait for user's semi to finish (user plays in app) ───────────────────────

echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
info "Waiting for you to finish your semi-final in the app"
info "(Play all 6 games — up to 10 minutes)"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""

if ! wait_for "$SILENT_SEMI_OUT" "tournament_semi_over" 600; then
  err_exit "User's semi didn't finish within 10 minutes."
fi
ok "Your semi-final is done!"

# ── wait for the final ────────────────────────────────────────────────────────

echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
info "The final is starting in the app — tap 'Idi u finale'!"
info "The finalist bot will NOT report; your app decides the winner."
info "(Up to 10 minutes)"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""

# Wait for tournament_over on any bot output file
TOURNAMENT_DONE=0
DEADLINE=$(( $(date +%s) + 600 ))
while (( $(date +%s) < DEADLINE )); do
  for f in "$TMP/b1.out" "$TMP/b2.out" "$TMP/b3.out"; do
    grep -q "tournament_over" "$f" 2>/dev/null && { TOURNAMENT_DONE=1; break 2; }
  done
  sleep 0.5
done

if [[ "$TOURNAMENT_DONE" -eq 0 ]]; then
  err_exit "Tournament_over not received within 10 minutes."
fi

echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
ok "Tournament complete! Check the app for the final results."
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
