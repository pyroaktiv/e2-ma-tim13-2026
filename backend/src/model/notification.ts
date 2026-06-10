export type NotificationCategory = "CET" | "RANGIRANJE" | "NAGRADE" | "OSTALO";

export interface NotificationRow {
  id: number;
  user_id: number;
  category: NotificationCategory;
  title: string;
  body: string;
  is_read: number;
  created_at: number;
}

export interface NotificationResponse {
  id: number;
  category: NotificationCategory;
  title: string;
  body: string;
  timestamp: string;
  is_read: boolean;
}
