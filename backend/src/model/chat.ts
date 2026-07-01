export interface ChatMessageRow {
  id: number;
  from_user_id: number;
  to_user_id: number;
  body: string;
  is_read: number;
  created_at: number;
}

export interface ChatConversationRow {
  other_id: number;
  username: string;
  avatar: string;
  last_body: string;
  last_at: number;
  unread_count: number;
}
