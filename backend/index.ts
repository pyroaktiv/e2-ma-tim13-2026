import { initDb } from "./src/db/schema";
import { db } from "./src/db/database";
import { json } from "./src/util/response";
import { verifyJWT } from "./src/util/jwt";
import { registerConnection, removeConnection, isOnline } from "./src/util/websocket";
import type { WsData } from "./src/util/websocket";
import { handleGameMessage, handleDisconnect } from "./src/game/registry";

import { handleRegister } from "./src/routes/auth/register";
import { handleVerifyEmail } from "./src/routes/auth/verify-email";
import { handleLogin } from "./src/routes/auth/login";
import { handleResetPassword } from "./src/routes/auth/reset-password";
import { handleLogout } from "./src/routes/auth/logout";
import { handleGetProfile } from "./src/routes/user/profile";
import { handleUpdateAvatar } from "./src/routes/user/avatar";
import { handleGetStats } from "./src/routes/user/stats";
import { handleGetNotifications, handleMarkAsRead } from "./src/routes/notifications";
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
    "/api/notifications/:id/read": { PATCH: handleMarkAsRead },
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
  },
  websocket: {
    open(ws) {
      registerConnection(ws.data.userId, ws);
    },
    message(ws, msg) {
      handleGameMessage(ws.data.userId, typeof msg === "string" ? msg : msg.toString());
    },
    close(ws) {
      removeConnection(ws.data.userId, ws);
      // Only treat as "left the game" once the user has no live connections.
      if (!isOnline(ws.data.userId)) handleDisconnect(ws.data.userId);
    },
  },
  async fetch(req, server) {
    const url = new URL(req.url);
    if (url.pathname === "/ws") {
      const token = url.searchParams.get("token");
      if (!token) return new Response("Unauthorized", { status: 401 });

      const payload = await verifyJWT(token);
      if (!payload) return new Response("Unauthorized", { status: 401 });

      const revoked = db
        .query("SELECT jti FROM revoked_tokens WHERE jti = ?")
        .get(payload.jti) as { jti: string } | null;
      if (revoked) return new Response("Unauthorized", { status: 401 });

      const upgraded = server.upgrade(req, { data: { userId: Number(payload.sub) } });
      if (upgraded) return undefined as unknown as Response;
      return new Response("WebSocket upgrade failed", { status: 400 });
    }
    return json({ error: "Not found" }, 404);
  },
});

console.log(`Server running on http://0.0.0.0:${port}`);
