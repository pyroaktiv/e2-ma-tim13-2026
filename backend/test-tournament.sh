#!/usr/bin/env bash
# test-tournament.sh — end-to-end test for spec 10 (tournaments)
#
# REST sections run unconditionally.
# The full 4-player bracket flow requires `websocat`:
#   cargo install websocat   OR   brew install websocat
#
# Usage:
#   bash test-tournament.sh [BASE_URL]
#   bash test-tournament.sh http://10.0.2.2:3000
#
# Defaults to http://localhost:3000.

set -euo pipefail

BASE="${1:-http://localhost:3000}"
WS_BASE="${BASE/http/ws}"   # http→ws, https→wss
PASS=0
FAIL=0
SKIP=0

TMPDIR_TEST=$(mktemp -d)
trap 'rm -rf "$TMPDIR_TEST"; kill $(jobs -p) 2>/dev/null || true' EXIT

# ── basic helpers ──────────────────────────────────────────────────────────────

GREEN='\033[0;32m'; RED='\033[0;31m'; YELLOW='\033[1;33m'; NC='\033[0m'

ok()   { echo -e "${GREEN}  PASS${NC} $1"; PASS=$((PASS+1)); }
fail() { echo -e "${RED}  FAIL${NC} $1"; FAIL=$((FAIL+1)); }
info() { echo -e "${YELLOW}  INFO${NC} $1"; }
skip() { echo -e "${YELLOW}  SKIP${NC} $1"; SKIP=$((SKIP+1)); }

post() {
  local url="$1" data="$2" token="${3:-}"
  local auth=(); [[ -n "$token" ]] && auth=(-H "Authorization: Bearer $token")
  curl -s -X POST "${auth[@]}" -H "Content-Type: application/json" -d "$data" "$BASE$url"
}

get() {
  local url="$1" token="${2:-}"
  local auth=(); [[ -n "$token" ]] && auth=(-H "Authorization: Bearer $token")
  curl -s "${auth[@]}" "$BASE$url"
}

login() {
  post /api/auth/login "{\"identifier\":\"$1\",\"password\":\"Password1\"}"
}

assert_eq() {
  local label="$1" json="$2" path="$3" expected="$4"
  local actual; actual=$(echo "$json" | jq -r "$path" 2>/dev/null)
  if [[ "$actual" == "$expected" ]]; then
    ok "$label"
  else
    fail "$label (expected='$expected' got='$actual')"
    echo "    JSON: $(echo "$json" | head -c 400)"; echo
  fi
}

assert_ge() {
  local label="$1" val="$2" min="$3"
  if (( val >= min )); then ok "$label"; else fail "$label (need >= $min, got $val)"; fi
}

# Wait up to timeout_s seconds for a pattern to appear in file.
wait_for() {
  local file="$1" pattern="$2" timeout_s="${3:-8}"
  local deadline=$(( $(date +%s) + timeout_s ))
  while (( $(date +%s) < deadline )); do
    grep -q "$pattern" "$file" 2>/dev/null && return 0
    sleep 0.2
  done
  return 1
}

# First JSON line whose type field matches.
extract_msg() {
  local file="$1" type="$2"
  grep -m1 "\"type\":\"$type\"" "$file" 2>/dev/null || echo '{}'
}

# ── WebSocket tournament flow (defined before main so it can be called below) ──

ws_send() {
  # Append a JSON line to a player's input file; tail -f picks it up and pipes to websocat.
  echo "$2" >> "$1"
}

ws_tournament_flow() {
  local TMP="$TMPDIR_TEST"
  local OUT1="$TMP/p1.out" OUT2="$TMP/p2.out" OUT3="$TMP/p3.out" OUT4="$TMP/p4.out"
  local IN1="$TMP/p1.in"  IN2="$TMP/p2.in"  IN3="$TMP/p3.in"  IN4="$TMP/p4.in"

  # Regular files — tail -f keeps stdin open to websocat across multiple writes.
  : > "$OUT1"; : > "$OUT2"; : > "$OUT3"; : > "$OUT4"
  : > "$IN1";  : > "$IN2";  : > "$IN3";  : > "$IN4"

  tail -f "$IN1" | stdbuf -oL websocat "$WS_BASE/ws?token=$T1" >> "$OUT1" 2>/dev/null & local WS1=$!
  tail -f "$IN2" | stdbuf -oL websocat "$WS_BASE/ws?token=$T2" >> "$OUT2" 2>/dev/null & local WS2=$!
  tail -f "$IN3" | stdbuf -oL websocat "$WS_BASE/ws?token=$T3" >> "$OUT3" 2>/dev/null & local WS3=$!
  tail -f "$IN4" | stdbuf -oL websocat "$WS_BASE/ws?token=$T4" >> "$OUT4" 2>/dev/null & local WS4=$!
  sleep 0.8  # allow all 4 WS connections to establish

  # ── queue all 4 ──────────────────────────────────────────────────────────────

  info "All 4 players sending find_tournament..."
  ws_send "$IN1" '{"type":"find_tournament"}'
  ws_send "$IN2" '{"type":"find_tournament"}'
  ws_send "$IN3" '{"type":"find_tournament"}'
  ws_send "$IN4" '{"type":"find_tournament"}'

  local all_found=1
  for i in 1 2 3 4; do
    local out; out=$(eval echo "\$OUT$i")
    if wait_for "$out" "tournament_found" 8; then
      ok "Player $i received tournament_found"
    else
      fail "Player $i did not receive tournament_found within 8s"
      all_found=0
    fi
  done
  (( all_found == 0 )) && { kill $WS1 $WS2 $WS3 $WS4 2>/dev/null; return; }

  # Tokens deducted.
  local NEW_TOK1; NEW_TOK1=$(get /api/user/profile "$T1" | jq -r '.tokens')
  local NEW_TOK2; NEW_TOK2=$(get /api/user/profile "$T2" | jq -r '.tokens')
  if (( NEW_TOK1 == TOK1 - 3 )); then ok "alice tokens $TOK1 → $NEW_TOK1 (−3 entry)"; else fail "alice tokens: expected $((TOK1-3)) got $NEW_TOK1"; fi
  if (( NEW_TOK2 == TOK2 - 3 )); then ok "bob tokens $TOK2 → $NEW_TOK2 (−3 entry)";   else fail "bob tokens: expected $((TOK2-3)) got $NEW_TOK2"; fi

  # Parse bracket + match details.
  local MSG1; MSG1=$(extract_msg "$OUT1" "tournament_found")
  local MSG3; MSG3=$(extract_msg "$OUT3" "tournament_found")

  local TOURN_ID;  TOURN_ID=$(echo "$MSG1"  | jq -r '.tournamentId // empty')
  local MID1;      MID1=$(echo "$MSG1"      | jq -r '.matchId // empty')
  local COLOR1;    COLOR1=$(echo "$MSG1"    | jq -r '.color // empty')
  local MID3;      MID3=$(echo "$MSG3"      | jq -r '.matchId // empty')
  local COLOR3;    COLOR3=$(echo "$MSG3"    | jq -r '.color // empty')
  local BLEN;      BLEN=$(echo "$MSG1"      | jq -r '.bracket | length')

  [[ -n "$TOURN_ID" ]] && ok "tournamentId present" || fail "tournamentId missing"
  [[ -n "$MID1" ]]     && ok "matchId in semi-1 present" || fail "matchId missing"
  [[ "$MID1" != "$MID3" ]] && ok "semi-1 and semi-2 have different matchIds" || fail "semi matchIds identical"
  assert_eq "bracket has 4 players"      "{\"n\":$BLEN}" '.n' "4"
  assert_eq "bracket[0].userId is a number" "$MSG1" '.bracket[0].userId | type' "number"
  assert_eq "bracket[0] has league.name"    "$MSG1" '.bracket[0].league | has("name")' "true"

  # REST: tournament in semifinal state.
  if [[ -n "$TOURN_ID" ]]; then
    local TS; TS=$(get "/api/tournaments/$TOURN_ID" "$T1")
    assert_eq "REST: status = semifinal" "$TS" '.status' "semifinal"
    assert_eq "REST: 4 participants"     "$TS" '.participants | length' "4"
  fi

  # ── semi-1 result (alice wins) ────────────────────────────────────────────────

  info "Reporting semi-1: alice wins..."
  local BS1 RS1
  [[ "$COLOR1" == "BLUE" ]] && { BS1=150; RS1=80; } || { BS1=80; RS1=150; }
  ws_send "$IN1" "{\"type\":\"report_result\",\"matchId\":\"$MID1\",\"blueScore\":$BS1,\"redScore\":$RS1,\"perGame\":[]}"

  for i in 1 2; do
    local out; out=$(eval echo "\$OUT$i")
    if wait_for "$out" "tournament_semi_over" 8; then ok "Player $i got tournament_semi_over"; else fail "Player $i missing tournament_semi_over"; fi
  done

  local S1W; S1W=$(extract_msg "$OUT1" "tournament_semi_over")
  local S1L; S1L=$(extract_msg "$OUT2" "tournament_semi_over")
  assert_eq "Semi-1 winner: won=true"         "$S1W" '.won' "true"
  local SEMI_TD; SEMI_TD=$(echo "$S1W" | jq -r '.rewards.tokensDelta // 0')
  assert_ge "Semi-1 winner: tokensDelta >= 2 (base +2, plus any star-bonus)" "$SEMI_TD" 2
  assert_eq "Semi-1 loser: won=false"         "$S1L" '.won' "false"
  assert_eq "Semi-1 loser: rewards=null"      "$S1L" '.rewards' "null"

  # ── semi-2 result (user3 wins) ────────────────────────────────────────────────

  info "Reporting semi-2: user3 wins..."
  local BS3 RS3
  [[ "$COLOR3" == "BLUE" ]] && { BS3=120; RS3=60; } || { BS3=60; RS3=120; }
  ws_send "$IN3" "{\"type\":\"report_result\",\"matchId\":\"$MID3\",\"blueScore\":$BS3,\"redScore\":$RS3,\"perGame\":[]}"

  for i in 3 4; do
    local out; out=$(eval echo "\$OUT$i")
    if wait_for "$out" "tournament_semi_over" 8; then ok "Player $i got tournament_semi_over"; else fail "Player $i missing tournament_semi_over"; fi
  done

  # Semi-losers receive tournament_update.
  for i in 2 4; do
    local out; out=$(eval echo "\$OUT$i")
    if wait_for "$out" "tournament_update" 5; then
      ok "Semi-loser $i got tournament_update (bracket observer)"
    else
      info "Semi-loser $i did not receive tournament_update (non-critical)"
    fi
  done

  # ── final (alice vs user3) ────────────────────────────────────────────────────

  info "Waiting for tournament_final_started on finalists..."
  for i in 1 3; do
    local out; out=$(eval echo "\$OUT$i")
    if wait_for "$out" "tournament_final_started" 8; then ok "Finalist $i got tournament_final_started"; else fail "Finalist $i missing tournament_final_started"; fi
  done

  local FMSG; FMSG=$(extract_msg "$OUT1" "tournament_final_started")
  local FMID; FMID=$(echo "$FMSG" | jq -r '.matchId // empty')
  local FCOL; FCOL=$(echo "$FMSG" | jq -r '.color // empty')

  [[ -n "$FMID" && "$FMID" != "$MID1" ]] && ok "Final has new matchId" || fail "Final matchId missing or reused"

  if [[ -n "$TOURN_ID" ]]; then
    local TS2; TS2=$(get "/api/tournaments/$TOURN_ID" "$T1")
    assert_eq "REST: status = final" "$TS2" '.status' "final"
  fi

  # alice wins final.
  info "Reporting final: alice wins..."
  local BF RF
  [[ "$FCOL" == "BLUE" ]] && { BF=200; RF=130; } || { BF=130; RF=200; }
  ws_send "$IN1" "{\"type\":\"report_result\",\"matchId\":\"$FMID\",\"blueScore\":$BF,\"redScore\":$RF,\"perGame\":[]}"

  # ── tournament_over ───────────────────────────────────────────────────────────

  info "Waiting for tournament_over on all 4..."
  for i in 1 2 3 4; do
    local out; out=$(eval echo "\$OUT$i")
    if wait_for "$out" "tournament_over" 8; then ok "Player $i got tournament_over"; else fail "Player $i missing tournament_over"; fi
  done

  local OVER; OVER=$(extract_msg "$OUT1" "tournament_over")
  assert_eq "tournament_over: .winner is object"    "$OVER" '.winner    | type' "object"
  assert_eq "tournament_over: .runner_up is object" "$OVER" '.runner_up | type' "object"

  local WIN_TD; WIN_TD=$(echo "$OVER" | jq -r '.winner.rewards.tokensDelta // 0')
  local WIN_SD; WIN_SD=$(echo "$OVER" | jq -r '.winner.rewards.starsDelta // 0')
  assert_ge "Final winner tokensDelta >= 3"  "$WIN_TD" 3
  assert_ge "Final winner starsDelta >= 20 (10 win + 10 bonus + score bonus)" "$WIN_SD" 20

  local RUN_SD; RUN_SD=$(echo "$OVER" | jq -r '.runner_up.rewards.starsDelta // -999')
  info "Runner-up starsDelta: $RUN_SD (regular loss formula: −10 + floor(score/40))"

  # REST reward check.
  local TOK1_F; TOK1_F=$(get /api/user/profile "$T1" | jq -r '.tokens')
  local STAR1_F; STAR1_F=$(get /api/user/profile "$T1" | jq -r '.total_stars')
  local MIN_TOK; MIN_TOK=$(( TOK1 - 3 + 2 + 3 ))  # entry−3, semi+2, final+3
  assert_ge "alice final tokens >= $MIN_TOK"         "$TOK1_F"  "$MIN_TOK"
  if (( STAR1_F > STARS1_BEFORE )); then
    ok "alice total_stars increased: $STARS1_BEFORE → $STAR1_F"
  else
    fail "alice total_stars did not increase (before=$STARS1_BEFORE after=$STAR1_F)"
  fi

  if [[ -n "$TOURN_ID" ]]; then
    local TS3; TS3=$(get "/api/tournaments/$TOURN_ID" "$T1")
    assert_eq "REST: status = finished" "$TS3" '.status' "finished"
    assert_eq "REST: all 4 results set" \
      "$TS3" '[.participants[] | select(.result != null)] | length' "4"
  fi

  # ── cancel from queue ─────────────────────────────────────────────────────────

  echo
  info "[Cancel from queue]"
  local OUT_Q="$TMP/q.out" IN_Q="$TMP/q.in"
  : > "$IN_Q"; : > "$OUT_Q"
  local TQ; TQ=$(login alice | jq -r '.token // empty')
  local TOK_Q; TOK_Q=$(get /api/user/profile "$TQ" | jq -r '.tokens')

  tail -f "$IN_Q" | stdbuf -oL websocat "$WS_BASE/ws?token=$TQ" >> "$OUT_Q" 2>/dev/null & local WSQ=$!
  sleep 0.3

  ws_send "$IN_Q" '{"type":"find_tournament"}'
  if wait_for "$OUT_Q" "tournament_queued" 5; then
    ok "Queue join: tournament_queued received"
    local TOK_Q2; TOK_Q2=$(get /api/user/profile "$TQ" | jq -r '.tokens')
    if (( TOK_Q2 == TOK_Q - 3 )); then
      ok "3 tokens deducted on join ($TOK_Q → $TOK_Q2)"
    else
      fail "Expected $((TOK_Q-3)) tokens after join, got $TOK_Q2"
    fi

    ws_send "$IN_Q" '{"type":"cancel_tournament"}'
    if wait_for "$OUT_Q" "tournament_cancelled" 5; then
      ok "Cancel: tournament_cancelled received"
      local TOK_Q3; TOK_Q3=$(get /api/user/profile "$TQ" | jq -r '.tokens')
      if (( TOK_Q3 == TOK_Q )); then
        ok "3 tokens refunded: restored to $TOK_Q3"
      else
        fail "Expected $TOK_Q tokens after refund, got $TOK_Q3"
      fi
    else
      fail "tournament_cancelled not received after cancel_tournament"
    fi
  else
    fail "tournament_queued not received after find_tournament"
  fi

  kill $WS1 $WS2 $WS3 $WS4 $WSQ 2>/dev/null || true
  wait $WS1 $WS2 $WS3 $WS4 $WSQ 2>/dev/null || true
}

# ── main ──────────────────────────────────────────────────────────────────────

echo
echo "=== Tournament — Integration Test ==="
echo "  Server : $BASE"
echo "  WS     : $WS_BASE"
echo

HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" "$BASE/api/leaderboard/weekly" 2>/dev/null || echo "000")
if [[ "$HTTP_CODE" == "000" ]]; then
  echo -e "${RED}FATAL${NC}: Server not reachable at $BASE"; exit 1
fi

# ── 1. Login 4 players ────────────────────────────────────────────────────────

echo "--- 1. Login ---"
L1=$(login alice);  T1=$(echo "$L1" | jq -r '.token // empty'); ID1=$(echo "$L1" | jq -r '.user.id // empty')
L2=$(login bob);    T2=$(echo "$L2" | jq -r '.token // empty'); ID2=$(echo "$L2" | jq -r '.user.id // empty')
L3=$(login user3);  T3=$(echo "$L3" | jq -r '.token // empty'); ID3=$(echo "$L3" | jq -r '.user.id // empty')
L4=$(login user4);  T4=$(echo "$L4" | jq -r '.token // empty'); ID4=$(echo "$L4" | jq -r '.user.id // empty')

for pair in "alice:$T1" "bob:$T2" "user3:$T3" "user4:$T4"; do
  local_name="${pair%%:*}"; local_tok="${pair##*:}"
  [[ -n "$local_tok" ]] && ok "Login $local_name" || { echo -e "${RED}FATAL${NC}: Cannot login $local_name"; exit 1; }
done

# ── 2. Pre-conditions ─────────────────────────────────────────────────────────

echo
echo "--- 2. Token pre-conditions ---"
post /api/test/restore-test-data '{}' "$T1" > /dev/null
info "Seed data restored (alice=10 tokens, others=5)"

L1=$(login alice);  T1=$(echo "$L1" | jq -r '.token // empty')
L2=$(login bob);    T2=$(echo "$L2" | jq -r '.token // empty')
L3=$(login user3);  T3=$(echo "$L3" | jq -r '.token // empty')
L4=$(login user4);  T4=$(echo "$L4" | jq -r '.token // empty')

TOK1=$(get /api/user/profile "$T1" | jq -r '.tokens')
TOK2=$(get /api/user/profile "$T2" | jq -r '.tokens')
TOK3=$(get /api/user/profile "$T3" | jq -r '.tokens')
TOK4=$(get /api/user/profile "$T4" | jq -r '.tokens')
STARS1_BEFORE=$(get /api/user/profile "$T1" | jq -r '.total_stars')

assert_ge "alice  >= 3 tokens ($TOK1)"  "$TOK1" 3
assert_ge "bob    >= 3 tokens ($TOK2)"  "$TOK2" 3
assert_ge "user3  >= 3 tokens ($TOK3)"  "$TOK3" 3
assert_ge "user4  >= 3 tokens ($TOK4)"  "$TOK4" 3
info "alice stars before tournament: $STARS1_BEFORE"

# ── 3. REST: GET /api/tournaments/:id ─────────────────────────────────────────

echo
echo "--- 3. REST: /api/tournaments/:id ---"
NOT_FOUND=$(get "/api/tournaments/00000000-0000-0000-0000-000000000000" "$T1")
assert_eq "404 for unknown tournament id" "$NOT_FOUND" '.error' "Tournament not found"

# ── 4. WebSocket flow ─────────────────────────────────────────────────────────

echo
echo "--- 4. WebSocket tournament flow ---"

if ! command -v websocat &> /dev/null; then
  skip "websocat not found — full bracket test skipped"
  skip "Install: cargo install websocat  then re-run"
  echo
  echo "  Manual verification:"
  echo "  1. Open 4 WS connections:  $WS_BASE/ws?token=<JWT>"
  echo "  2. All 4 send:             {\"type\":\"find_tournament\"}"
  echo "  3. Each receives:          tournament_found  (bracket + matchId + color + content)"
  echo "  4. Play via match_move;    one player per semi sends report_result"
  echo "  5. Both semis done →       tournament_final_started (finalists)"
  echo "                             tournament_update         (losers)"
  echo "  6. Final: report_result →  tournament_over  (all 4, with rewards)"
else
  ws_tournament_flow
fi

# ── 5. win_tournament mission (via test trigger) ───────────────────────────────

echo
echo "--- 5. win_tournament daily mission ---"
post /api/test/reset-daily-missions "{\"user_id\":$ID1}" "$T1" > /dev/null
TRIG=$(post /api/test/trigger-mission "{\"user_id\":$ID1,\"mission_key\":\"win_tournament\"}" "$T1")
if echo "$TRIG" | jq -e '.ok' > /dev/null 2>&1; then
  ok "Test trigger win_tournament accepted"
  MISSIONS=$(get /api/missions/daily "$T1")
  assert_eq "win_tournament mission completed" \
    "$MISSIONS" '.missions[] | select(.key=="win_tournament") | .completed' "true"
else
  skip "Test trigger unavailable: $(echo "$TRIG" | jq -r '.error // "unknown"')"
fi

# ── summary ───────────────────────────────────────────────────────────────────

echo
echo "==========================================="
echo -e "  Results: ${GREEN}${PASS} passed${NC}, ${RED}${FAIL} failed${NC}, ${YELLOW}${SKIP} skipped${NC}"
echo "==========================================="
echo
[[ $FAIL -eq 0 ]]
