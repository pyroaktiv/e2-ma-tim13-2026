export interface FriendRequestRow {
  id: number;
  from_user_id: number;
  to_user_id: number;
  status: "pending" | "accepted" | "declined";
  created_at: number;
}

export interface GameInviteRow {
  id: number;
  from_user_id: number;
  to_user_id: number;
  status: "pending" | "accepted" | "declined" | "cancelled" | "expired";
  created_at: number;
  expires_at: number;
}

export interface FriendProfile {
  id: number;
  username: string;
  avatar: string;
  total_stars: number;
  league: { name: string; icon: string };
  monthly_rank: null; // TODO: implement with ranking system
  is_online: boolean;
  in_game: boolean;
  friendship_id: number;
}
