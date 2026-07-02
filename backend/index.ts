import { initDb } from "./src/db/schema";
import { seedGameContent } from "./src/db/seed-content";
import { seedUsers } from "./src/db/seed-users";
import { db } from "./src/db/database";
import { json } from "./src/util/response";
import { verifyJWT } from "./src/util/jwt";
import { registerConnection, removeConnection } from "./src/util/websocket";
import type { WsData } from "./src/util/websocket";
import { onSocketMessage, onSocketClose } from "./src/match/manager";
import {
  onSocketMessage as onChallengeSocketMessage,
  onSocketClose as onChallengeSocketClose,
} from "./src/challenge/manager";
import {
  onSocketMessage as onTournamentSocketMessage,
  onSocketClose as onTournamentSocketClose,
} from "./src/tournament/manager";
import { handleGetTournament } from "./src/routes/tournaments";
import { handleListChallenges, handleGetChallenge } from "./src/routes/challenges";
import { onSocketMessage as onChatSocketMessage } from "./src/chat/manager";
import { handleGetConversations, handleGetMessages, handleSearchChatUsers } from "./src/routes/chat";

import { handleRegister } from "./src/routes/auth/register";
import { handleVerifyEmail } from "./src/routes/auth/verify-email";
import { handleLogin } from "./src/routes/auth/login";
import { handleResetPassword } from "./src/routes/auth/reset-password";
import { handleLogout } from "./src/routes/auth/logout";
import { handleGetProfile } from "./src/routes/user/profile";
import { handleUpdateAvatar } from "./src/routes/user/avatar";
import { handleGetStats } from "./src/routes/user/stats";
import { handleGetNotifications, handleMarkAsRead, handleRegisterFcmToken, handleUnregisterFcmToken } from "./src/routes/notifications";
import { handleGetRegions, handleGetRegionMap, handleGetRegionStats, handleGetRegionList } from "./src/routes/regions/index";
import { handleSubmitGameResult } from "./src/routes/game/result";
import { handleGetWeeklyLeaderboard, handleGetMonthlyLeaderboard } from "./src/routes/leaderboard/index";
import { handleGetDailyMissions } from "./src/routes/missions";
import { handleForceWeeklyReset, handleForceMonthlyReset, handleSetUserStars, handleRestoreTestData, handleTriggerMission, handleResetDailyMissions } from "./src/routes/test/index";
import { checkAndRunWeeklyReset } from "./src/util/weekly";
import { checkAndRunMonthlyReset } from "./src/util/monthly";
import { handleGetFriends } from "./src/routes/friends/list";
import { handleSearchUsers } from "./src/routes/friends/search";
import { handleRemoveFriend } from "./src/routes/friends/remove";
import {
  handleSendFriendRequest,
  handleGetFriendRequests,
  handleAcceptFriendRequest,
  handleDeclineFriendRequest,
} from "./src/routes/friends/requests";
import {
  handleSendGameInvite,
  handleGetGameInvites,
  handleGetSentGameInvites,
  handleAcceptGameInvite,
  handleDeclineGameInvite,
  handleCancelGameInvite,
} from "./src/routes/friends/invites";

initDb();
seedGameContent();
await seedUsers();

checkAndRunWeeklyReset();
checkAndRunMonthlyReset();

setInterval(
  () => {
    checkAndRunWeeklyReset();
    checkAndRunMonthlyReset();
  },
  60 * 60 * 1000,
);

setInterval(
  () => {
    db.run("DELETE FROM revoked_tokens WHERE expires_at < ?", [
      Math.floor(Date.now() / 1000),
    ]);
  },
  60 * 60 * 1000,
);

const port = Number(process.env.PORT ?? 3000);

Bun.serve<WsData>({
  hostname: "0.0.0.0",
  port,
  routes: {
    "/api/auth/register": { POST: handleRegister },
    "/api/auth/verify-email": { GET: handleVerifyEmail },
    "/api/auth/login": { POST: handleLogin },
    "/api/auth/reset-password": { POST: handleResetPassword },
    "/api/auth/logout": { POST: handleLogout },
    "/api/user/profile": { GET: handleGetProfile },
    "/api/user/avatar": { PUT: handleUpdateAvatar },
    "/api/user/stats": { GET: handleGetStats },
    "/api/notifications": { GET: handleGetNotifications },
    "/api/notifications/fcm-token": {
      POST: handleRegisterFcmToken,
      DELETE: handleUnregisterFcmToken,
    },
    "/api/notifications/:id/read": { PATCH: handleMarkAsRead },
    "/api/regions/list": { GET: handleGetRegionList },
    "/api/regions": { GET: handleGetRegions },
    "/api/regions/map": { GET: handleGetRegionMap },
    "/api/regions/:name/stats": { GET: handleGetRegionStats },
    "/api/game/result": { POST: handleSubmitGameResult },
    "/api/leaderboard/weekly": { GET: handleGetWeeklyLeaderboard },
    "/api/leaderboard/monthly": { GET: handleGetMonthlyLeaderboard },
    "/api/test/force-weekly-reset":  { POST: handleForceWeeklyReset },
    "/api/test/force-monthly-reset": { POST: handleForceMonthlyReset },
    "/api/test/set-user-stars":      { POST: handleSetUserStars },
    "/api/test/restore-test-data":   { POST: handleRestoreTestData },
    "/api/test/trigger-mission":     { POST: handleTriggerMission },
    "/api/test/reset-daily-missions": { POST: handleResetDailyMissions },
    "/api/friends": { GET: handleGetFriends },
    "/api/friends/search": { GET: handleSearchUsers },
    "/api/friends/:id": { DELETE: handleRemoveFriend },
    "/api/friends/requests": {
      POST: handleSendFriendRequest,
      GET: handleGetFriendRequests,
    },
    "/api/friends/requests/:id/accept": { POST: handleAcceptFriendRequest },
    "/api/friends/requests/:id/decline": { POST: handleDeclineFriendRequest },
    "/api/friends/invites": {
      POST: handleSendGameInvite,
      GET: handleGetGameInvites,
    },
    "/api/friends/invites/sent": { GET: handleGetSentGameInvites },
    "/api/friends/invites/:id/accept": { POST: handleAcceptGameInvite },
    "/api/friends/invites/:id/decline": { POST: handleDeclineGameInvite },
    "/api/friends/invites/:id": { DELETE: handleCancelGameInvite },
    "/api/missions/daily": { GET: handleGetDailyMissions },
    "/api/challenges": { GET: handleListChallenges },
    "/api/challenges/:id": { GET: handleGetChallenge },
    "/api/tournaments/:id": { GET: handleGetTournament },
    "/api/chat/conversations": { GET: handleGetConversations },
    "/api/chat/messages/:id": { GET: handleGetMessages },
    "/api/chat/search": { GET: handleSearchChatUsers },
  },
  websocket: {
    open(ws) {
      if (ws.data.kind === "user") registerConnection(ws.data.userId, ws);
    },
    message(ws, msg) {
      onSocketMessage(ws, msg);
      onChallengeSocketMessage(ws, msg);
      onTournamentSocketMessage(ws, msg);
      onChatSocketMessage(ws, msg);
    },
    close(ws) {
      if (ws.data.kind === "user") removeConnection(ws.data.userId, ws);
      onSocketClose(ws);
      onChallengeSocketClose(ws);
      onTournamentSocketClose(ws);
    },
  },
  async fetch(req, server) {
    const url = new URL(req.url);
    if (url.pathname === "/ws") {
      const token = url.searchParams.get("token");

      if (token) {
        const payload = await verifyJWT(token);
        if (!payload) return new Response("Unauthorized", { status: 401 });

        const revoked = db
          .query("SELECT jti FROM revoked_tokens WHERE jti = ?")
          .get(payload.jti) as { jti: string } | null;
        if (revoked) return new Response("Unauthorized", { status: 401 });

        const data: WsData = {
          kind: "user",
          userId: Number(payload.sub),
          username: payload.username,
        };
        const upgraded = server.upgrade(req, { data });
        if (upgraded) return undefined as unknown as Response;
        return new Response("WebSocket upgrade failed", { status: 400 });
      }

      // Gost — bez tokena, dobija privremeni identitet (može da igra, bez nagrada).
      if (url.searchParams.get("guest") === "1") {
        const data: WsData = {
          kind: "guest",
          guestId: crypto.randomUUID(),
          username: "Gost",
        };
        const upgraded = server.upgrade(req, { data });
        if (upgraded) return undefined as unknown as Response;
        return new Response("WebSocket upgrade failed", { status: 400 });
      }

      return new Response("Unauthorized", { status: 401 });
    }
    return json({ error: "Not found" }, 404);
  },
});

console.log(`Server running on http://0.0.0.0:${port}`);
