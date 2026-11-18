export interface EventCardInfo {
  id: number;
  venueId: number;
  sportId?: number;
  cityId?: number;
  name: string;
  description: string;
  sportName: string;
  venueName: string;
  latitude: number;
  longitude: number;
  scheduledAt: string;
  maxParticipants: number;
  participants: number[];
  participantCount: number;
  status?: string;
  expectedDurationMin: number;
  minSkillLevelId: number;
  distance_km: number | null;
}

export type CreateEventInfo = Omit<EventDetails, "id" | "participants" | "status" | "createdAt">;
export interface EventDetails extends EventCardInfo {
  organizerId: number;
  organizerUsername: string;
  minAge: number;
  maxAge: number;
  createdAt: string;
}

export interface EventFilters {
  sportId?: number[],
  cityId?: number[],
  distance_km?: number,
  scheduledAt?: string;
  endingBefore?: number;
}

