import { dummyEvents, currentUserId, preferedUserLocation } from "@/dummy_data/simple_dummy_data.ts";
import type {EventCardInfo, CreateEventInfo, EventDetails} from "@/types/event.ts";
import {calculateDistanceKm, getUserLocation, type UserLocation} from "@/lib/geolocation.ts";

// const USE_DUMMY_DATA = import.meta.env.APP_USE_DUMMY_DATA === "true";
const USE_DUMMY_DATA = true;

async function fetchData<T>(url: string, options?: RequestInit): Promise<T> {
  const response = await fetch(url, options);

  if (!response.ok) {
    throw new Error(`Request failed: ${response.status}`);
  }

  return (await response.json()) as T;
}

async function getUserPreferredLocation(): Promise<UserLocation> {
  let userLoc: UserLocation = preferedUserLocation;
  try {
    userLoc = await getUserLocation();
  } catch (error) {
    console.error(error);
    userLoc.accuracyRadiusMeters = 9999999;
  }
  // Ako je preciznost dobivene lokacije veći od +- 1km
  if (userLoc.accuracyRadiusMeters > 1000) {
    userLoc = preferedUserLocation;
  }

  return userLoc;
}

function roundSometimes(x: number) {
  if (x < 1) return Math.round(x * 10) / 10;
  else return Math.round(x);
}

export async function getEvents(): Promise<EventCardInfo[]> {
  if (USE_DUMMY_DATA) {
    const userLoc = await getUserPreferredLocation();

    return dummyEvents.map(event => ({
      ...event,
      distance_km: roundSometimes(calculateDistanceKm(userLoc.latitude,
                   userLoc.longitude, event.latitude, event.longitude)),
    }));
  }

  return fetchData<EventCardInfo[]>("/api/v1/events");
}

export async function getMyEvents(): Promise<EventCardInfo[]> {
  if (USE_DUMMY_DATA) {
    const userLoc = await getUserPreferredLocation();

    return dummyEvents.map(event => ({
      ...event,
      distance_km: roundSometimes(calculateDistanceKm(userLoc.latitude,
        userLoc.longitude, event.latitude, event.longitude)),
    }))
    .filter(event =>
      event.participants.includes(currentUserId)
    );
  }

  return fetchData<EventCardInfo[]>("/api/users/me/events");
}

export async function getEventDetails(id: number): Promise<EventDetails> {
  if (USE_DUMMY_DATA) {
    const userLoc = await getUserPreferredLocation();

    const event: EventDetails = dummyEvents.find(el => el.id === id)!;
    event.distance_km = roundSometimes(calculateDistanceKm(userLoc.latitude,
      userLoc.longitude, event.latitude, event.longitude));

    return event;
  }

  return fetchData<EventDetails>(`/api/v1/events/${id}`);
}

export async function createEvent(data: CreateEventInfo): Promise<EventCardInfo> {
  if (USE_DUMMY_DATA) {
    const event: EventDetails = {
      ...data,
      id: Math.max(0, ...dummyEvents.map(e => e.id)) + 1,
      participants: [],
      cancelled_at: null,
      created_at: Date.now().toString(),
    };

    dummyEvents.push(event);

    return event;
  }

  return fetchData<EventCardInfo>("/api/v1/events", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    body: JSON.stringify(data),
  });
}

export async function joinEvent(eventId: number): Promise<void> {
  if (USE_DUMMY_DATA) {
    const event = dummyEvents.find(e => e.id === eventId);

    if (!event) throw new Error("Event not found");

    if (event.participants.includes(currentUserId)) {
      throw new Error("Already joined");
    }

    if (event.participants.length >= event.max_participants) {
      throw new Error("Event is full");
    }

    event.participants.push(currentUserId);

    return;
  }

  const response = await fetch(`/api/v1/events/${eventId}/members`, {
    method: "POST",
  });

  if (!response.ok) {
    throw new Error(`Failed to join event: ${response.status}`);
  }
}
