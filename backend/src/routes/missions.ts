import { requireAuth } from "../middleware/auth";
import { json } from "../util/response";
import { getMissionsForUser } from "../util/missions";

export async function handleGetDailyMissions(req: Request): Promise<Response> {
  const auth = await requireAuth(req);
  if (auth instanceof Response) return auth;

  return json(getMissionsForUser(auth.user_id));
}
