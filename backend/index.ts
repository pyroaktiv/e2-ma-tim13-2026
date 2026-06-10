import { initDb } from "./src/db/schema";
import { db } from "./src/db/database";
import { json } from "./src/util/response";

import { handleRegister } from "./src/routes/auth/register";
import { handleVerifyEmail } from "./src/routes/auth/verify-email";
import { handleLogin } from "./src/routes/auth/login";
import { handleResetPassword } from "./src/routes/auth/reset-password";
import { handleLogout } from "./src/routes/auth/logout";
import { handleGetProfile } from "./src/routes/user/profile";
import { handleUpdateAvatar } from "./src/routes/user/avatar";
import { handleGetStats } from "./src/routes/user/stats";

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

Bun.serve({
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
  },
  fetch(_) {
    return json({ error: "Not found" }, 404);
  },
});

console.log(`Server running on http://0.0.0.0:${port}`);
