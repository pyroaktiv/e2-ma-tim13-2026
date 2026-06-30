export interface UserRow {
  id: number;
  email: string;
  username: string;
  password_hash: string;
  region: string;
  avatar: string;
  tokens: number;
  total_stars: number;
  league_id: number;
  qr_token: string;
  email_verified: number;
  created_at: number;
  updated_at: number;
}

export interface UserProfile {
  id: number;
  username: string;
  email: string;
  avatar: string;
  tokens: number;
  total_stars: number;
  league: { name: string; icon: string };
  region: string;
  qr_token: string;
  /** Mesto regiona u prethodnom mesečnom ciklusu (1/2/3) — boja okvira avatara (spec 5.e); null inače. */
  region_medal: number | null;
}
