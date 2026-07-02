#!/usr/bin/env bash
# Tests Feature 4 (Leaderboard) without needing multiple emulators.
# Run with: bash backend/test-leaderboard.sh
# The backend must be running: cd backend && bun run index.ts

set -euo pipefail

BASE="${BASE_URL:-http://localhost:3000}"
PASS=0
FAIL=0

GREEN='\033[0;32m'
RED='\033[0;31m'
NC='\033[0m'

check() {
  local label="$1"
  local body="$2"
  local pattern="$3"
  if echo "$body" | grep -q "$pattern"; then
    echo -e "${GREEN}PASS${NC}: $label"
    PASS=$((PASS + 1))
  else
    echo -e "${RED}FAIL${NC}: $label"
    echo "       Pattern: $pattern"
    echo "       Got:     $(echo "$body" | head -c 200)"
    FAIL=$((FAIL + 1))
  fi
}

check_absent() {
  local label="$1"
  local body="$2"
  local pattern="$3"
  if ! echo "$body" | grep -q "$pattern"; then
    echo -e "${GREEN}PASS${NC}: $label"
    PASS=$((PASS + 1))
  else
    echo -e "${RED}FAIL${NC}: $label (pattern should be absent)"
    echo "       Pattern: $pattern"
    FAIL=$((FAIL + 1))
  fi
}

echo "=== Leaderboard Test Suite ==="
echo "Backend: $BASE"
echo ""

# --- 1. Server health check ---
echo "--- Server check ---"
if ! curl -sf "$BASE/api/leaderboard/weekly" -o /dev/null -w "" 2>/dev/null; then
  # Any response (even 401) means the server is up
  HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" "$BASE/api/leaderboard/weekly")
  if [ "$HTTP_CODE" = "000" ]; then
    echo "ERROR: Backend not reachable at $BASE. Start it with: cd backend && bun run index.ts"
    exit 1
  fi
fi
echo "Backend is up."
echo ""

# --- 2. Login as alice ---
echo "--- Auth ---"
LOGIN=$(curl -sf -X POST "$BASE/api/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"identifier":"alice@example.com","password":"Password1"}')
check "Login as alice" "$LOGIN" '"token"'

TOKEN=$(echo "$LOGIN" | grep -o '"token":"[^"]*"' | cut -d'"' -f4)
ALICE_ID=$(echo "$LOGIN" | grep -o '"id":[0-9]*' | head -1 | cut -d: -f2)
echo "  Alice ID=$ALICE_ID, token=${TOKEN:0:30}..."
echo ""

# --- 3. Restore all test users to seed stats (alice at rank 1, user3–user12 populated) ---
echo "--- Setup: restore test leaderboard data ---"
RESTORE=$(curl -sf -X POST "$BASE/api/test/restore-test-data")
check "restore-test-data accepted" "$RESTORE" '"ok":true'
UPDATED=$(echo "$RESTORE" | grep -o '"updated":[0-9]*' | cut -d: -f2)
echo "  Rows updated: $UPDATED (expect 11)"
echo ""

# --- 4. Weekly leaderboard ---
echo "--- Weekly leaderboard ---"
WEEKLY=$(curl -sf "$BASE/api/leaderboard/weekly" \
  -H "Authorization: Bearer $TOKEN")
check "Has entries array"  "$WEEKLY" '"entries"'
check "Has cycle_start"    "$WEEKLY" '"cycle_start"'
check "Has cycle_end"      "$WEEKLY" '"cycle_end"'
check "Has my_rank"        "$WEEKLY" '"my_rank"'
check "alice is rank 1"    "$WEEKLY" '"rank":1'
check "alice appears"      "$WEEKLY" '"username":"alice"'
ENTRY_COUNT=$(echo "$WEEKLY" | grep -o '"rank"' | wc -l | tr -d ' ')
echo "  Entries on weekly board: $ENTRY_COUNT (expect 11 — bob has 0 games)"

# --- 5. bob must NOT appear (0 games played — spec §4a) ---
check_absent "bob absent from weekly board" "$WEEKLY" '"username":"bob"'
echo ""

# --- 6. Monthly leaderboard ---
echo "--- Monthly leaderboard ---"
MONTHLY=$(curl -sf "$BASE/api/leaderboard/monthly" \
  -H "Authorization: Bearer $TOKEN")
check "Has entries array"  "$MONTHLY" '"entries"'
check "Has cycle_start"    "$MONTHLY" '"cycle_start"'
check "Has cycle_end"      "$MONTHLY" '"cycle_end"'
check "alice is rank 1"    "$MONTHLY" '"rank":1'
check_absent "bob absent from monthly board" "$MONTHLY" '"username":"bob"'
echo ""

# --- 7. Force weekly reset and check rewards ---
echo "--- Weekly reset ---"
WRESET=$(curl -sf -X POST "$BASE/api/test/force-weekly-reset")
check "Reset endpoint returns ok"   "$WRESET" '"ok":true'
check "Reset message present"       "$WRESET" '"message"'

NOTIFS=$(curl -sf "$BASE/api/notifications" \
  -H "Authorization: Bearer $TOKEN")
check "Alice got a reward notification (NAGRADE)"  "$NOTIFS" 'NAGRADE'
check "Notification mentions weekly tournament"    "$NOTIFS" 'Nedeljni'
check "Notification mentions tokens"               "$NOTIFS" "žeton"

WEEKLY_POST=$(curl -sf "$BASE/api/leaderboard/weekly" \
  -H "Authorization: Bearer $TOKEN")
check "Weekly board empty after reset" "$WEEKLY_POST" '"entries":\[\]'
echo ""

# --- 8. Re-populate all test users for monthly reset test ---
echo "--- Monthly reset ---"
curl -sf -X POST "$BASE/api/test/restore-test-data" > /dev/null

MRESET=$(curl -sf -X POST "$BASE/api/test/force-monthly-reset")
check "Monthly reset endpoint returns ok"  "$MRESET" '"ok":true'

NOTIFS2=$(curl -sf "$BASE/api/notifications" \
  -H "Authorization: Bearer $TOKEN")
check "Alice got monthly reward notification"  "$NOTIFS2" 'Mesečni'
check "Monthly notification mentions tokens"  "$NOTIFS2" "žeton"

MONTHLY_POST=$(curl -sf "$BASE/api/leaderboard/monthly" \
  -H "Authorization: Bearer $TOKEN")
check "Monthly board empty after reset" "$MONTHLY_POST" '"entries":\[\]'
echo ""

# --- 9. Summary ---
TOTAL=$((PASS + FAIL))
echo "=== Results: $PASS/$TOTAL passed ==="
if [ "$FAIL" -gt 0 ]; then
  echo -e "${RED}$FAIL test(s) failed.${NC}"
  exit 1
else
  echo -e "${GREEN}All tests passed.${NC}"
fi
echo ""
echo "Visual test (one emulator):"
echo "  1. Login as alice / Password1"
echo "  2. Open Leaderboard → see 11 ranked users, cycle dates, league icons"
echo "  3. Run: curl -X POST $BASE/api/test/force-weekly-reset"
echo "  4. In the app, navigate away and back → reward dialog should appear with animation"
echo "  5. Dismiss dialog → notification marked read"
echo "  6. Re-run the cycle to test reward dialog a second time:"
echo "     curl -X POST $BASE/api/test/restore-test-data"
echo "     curl -X POST $BASE/api/test/force-weekly-reset"
