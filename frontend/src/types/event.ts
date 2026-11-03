export interface EventCardInfo {
  id: number;
  name: string;
  description: string;
  sportName: string;
  locationName: string;
  latitude: number;
  longitude: number;
  start_date: string;
  max_participants: number;
  participants: number[];
  distance_km: number | null;
}

export type CreateEventInfo = Omit<EventDetails, "id" | "participants" | "cancelled_at" | "created_at">;
export interface EventDetails extends EventCardInfo {
  venue_id: number;
  created_by_id: number;
  scheduled_at: string;
  expected_duration_min: number;
  min_age: number;
  max_age: number;
  min_skill_level_id: number;
  cancelled_at: string | null;
  created_at: string;
}
