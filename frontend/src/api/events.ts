import { dummyEvents, dummyUsers, currentUserId, preferedUserLocation } from "@/dummy_data/simple_dummy_data.ts";
import type {EventCardInfo, CreateEventInfo, EventDetails} from "@/types/event.ts";
import type {PublicUserInfo} from "@/types/user.ts";
import {calculateDistanceKm, getUserLocation, type UserLocation} from "@/lib/geolocation.ts";

// const USE_DUMMY_DATA = import.meta.env.APP_USE_DUMMY_DATA === "true";
const USE_DUMMY_DATA = true;

const API_PREFIX = `/api/v1`;

async function fetchData<T>(url: string, options?: RequestInit): Promise<T> {
  const response = await fetch(API_PREFIX + url, options);

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

  return fetchData<EventCardInfo[]>("/events");
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

  return fetchData<EventCardInfo[]>("/users/me/events");
}

export async function getEventDetails(id: number): Promise<EventDetails> {
  if (USE_DUMMY_DATA) {
    const userLoc = await getUserPreferredLocation();

    const event: EventDetails = dummyEvents.find(el => el.id === id)!;
    event.distance_km = roundSometimes(calculateDistanceKm(userLoc.latitude,
      userLoc.longitude, event.latitude, event.longitude));

    return event;
  }

  return fetchData<EventDetails>(`/events/${id}`);
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

  return fetchData<EventCardInfo>("/events", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    body: JSON.stringify(data),
  });
}

// TODO: Figure out edit
export async function editEvent(data: CreateEventInfo): Promise<EventCardInfo> {
  if (USE_DUMMY_DATA) {
    throw new Error("NOT IMPLEMENTED");
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

  return fetchData<EventCardInfo>("/events", {
    method: "PATCH",
    headers: {
      "Content-Type": "application/json",
    },
    body: JSON.stringify(data),
  });
}

export async function cancelEvent(eventId: number): Promise<void> {
  if (USE_DUMMY_DATA) {
    const eventIdx = dummyEvents.findIndex(e => e.id === eventId);
    if (eventIdx === -1) {
      throw new Error("Event cannot be canceled as it does not exist!");
    }

    const event = dummyEvents[eventIdx];
    if (event.created_by_id !== currentUserId) {
      throw new Error("Permission denied! You cannot cancel this event!");
    }

    dummyEvents.splice(eventIdx, 1);
    return;
  }

  const response = await fetch(`${API_PREFIX}/events/${eventId}/cancel`, {
    method: "POST",
  });

  if (!response.ok) {
    throw new Error("Event cannot be canceled: " + response.status)
  }
}

// TODO: Figure out join
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

  const response = await fetch(`${API_PREFIX}/events/${eventId}/members`, {
    method: "POST",
  });

  if (!response.ok) {
    throw new Error(`Failed to join event: ${response.status}`);
  }
}

// TODO: Figure out leave
export async function leaveEvent(eventId: number): Promise<void> {
  if (USE_DUMMY_DATA) {
    throw new Error("NOT IMPLEMENTED");
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

  const response = await fetch(`${API_PREFIX}/events/${eventId}/members/me`, {
    method: "DELETE",
  });

  if (!response.ok) {
    throw new Error(`Failed to leave event: ${response.status}`);
  }
}

export async function getEventMembers(eventId: number): Promise<PublicUserInfo[]> {
  if (USE_DUMMY_DATA) {
    const event = dummyEvents.find(e => e.id === eventId);

    if (!event) throw new Error("Event not found");

    return event.participants.map(userId => {
      const user = dummyUsers.find(u => u.id === userId);
      if (!user) {
        return
      }

      return user;
    });
  }

  return fetchData<PublicUserInfo[]>(`/events/${eventId}/members`);
}
