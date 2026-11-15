export interface EventCardInfo {
  id: number;
  venueId: number;
  sportId?: number;
  name: string;
  description: string;
  sportName: string;
  locationName: string;
  latitude: number;
  longitude: number;
  scheduledAt: string;
  maxParticipants: number;
  participants: number[];
  participantCount: number;
  distance_km: number | null;
}

export type CreateEventInfo = Omit<EventDetails, "id" | "participants" | "status" | "createdAt">;
export interface EventDetails extends EventCardInfo {
  organizerId: number;
  expectedDurationMin: number;
  minAge: number;
  maxAge: number;
  minSkillLevelId: number;
  status: string | null;
  createdAt: string;
}
