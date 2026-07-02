export type MissionKey = "win_match" | "send_chat" | "friendly_match" | "win_tournament";

export interface MissionStatus {
  key: MissionKey;
  title: string;
  completed: boolean;
  stars_reward: number;
}

export interface DailyMissionsResponse {
  date: string;
  missions: MissionStatus[];
  bonus: { all_complete: boolean; tokens_reward: number; stars_reward: number };
}
