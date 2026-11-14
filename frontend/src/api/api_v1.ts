import { dummyEvents, dummyUsers, currentUserId, preferedUserLocation } from "@/dummy_data/simple_dummy_data.ts";
import type {EventCardInfo, CreateEventInfo, EventDetails} from "@/types/event.ts";
import type {PublicUserInfo, UserDetails, UserCreateDetails} from "@/types/user.ts";
import {calculateDistanceKm, getUserLocation, type UserLocation} from "@/lib/geolocation.ts";
import { getDefaultAvatar } from "@/lib/utils";

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

/*
 *  Event api
 *  /event/*
 */

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
    }) as PublicUserInfo[];
  }

  return fetchData<PublicUserInfo[]>(`/events/${eventId}/members`);
}



/*
 *  User api
 *  /user/*
 */


export async function getUserProfile(): Promise<UserDetails> {
  if (USE_DUMMY_DATA) {
    const user = dummyUsers.find(u => u.id === currentUserId);

    if (!user) {
      throw new Error("User profile not found!")
    }

    return user;
  }

  return fetchData<UserDetails>(`/users/me`);
}


export async function editUserProfile(newUserInfo: UserCreateDetails): Promise<void> {
  if (USE_DUMMY_DATA) {
    const userIdx = dummyUsers.findIndex(u => u.id === currentUserId);

    if (userIdx === -1) {
      throw new Error("User profile not found!")
    }

    dummyUsers[userIdx] = newUserInfo;

    return;
  }


  const response = await fetch(`${API_PREFIX}/users/me`, {
    method: "PATCH",
    headers: {
      "Content-Type": "application/json",
    },
    body: JSON.stringify(newUserInfo)
  });

  if (!response.ok) {
    throw new Error(`Cannot edit user profile: ${response.status}`)
  }
}

export async function uploadUserProfilePhoto(url: string): Promise<void> {
  if (USE_DUMMY_DATA) {
    const userIdx = dummyUsers.findIndex(u => u.id === currentUserId);

    if (userIdx === -1) {
      throw new Error("User profile not found!")
    }

    dummyUsers[userIdx].profile_photo_url = getDefaultAvatar(url);

    return;
  }

  const response = await fetch(`${API_PREFIX}/users/me/photo`, {
    method: "PUT",
    headers: {
      "Content-Type": "Multipart: file", // Otkriti kako ovo treba izgledati
    },
    body: "TODO: image data"
  });

  if (!response.ok) {
    throw new Error(`Cannot edit user profile: ${response.status}`)
  }
}

export async function getUsers(sportId: number | null, cityId: number | null, ageMin: number | null, ageMax: number | null, skillLevelId: number | null ): Promise<PublicUserInfo[]> {
  if (USE_DUMMY_DATA) {
    function calculateAge(dateOfBirth: string): number {
      const birthDate = new Date(dateOfBirth);
      const today = new Date();

      let age = today.getFullYear() - birthDate.getFullYear();

      const birthdayNotReached =
        today.getMonth() < birthDate.getMonth() ||
        (today.getMonth() === birthDate.getMonth() &&
          today.getDate() < birthDate.getDate());

      if (birthdayNotReached) age--;

      return age;
    }

    const users = dummyUsers.filter(u =>
      (!sportId || u.user_sports.map(s => s.sport_id).includes(sportId))
      && (!cityId || u.city_id === cityId)
      && (!ageMin || u.date_of_birth && calculateAge(u.date_of_birth) >= ageMin)
      && (!ageMax || u.date_of_birth && calculateAge(u.date_of_birth) <= ageMax)
      && ((!sportId || !skillLevelId) || u.user_sports.filter(s => s.sport_id === sportId && s.skill_level_id >= skillLevelId).length > 0)
    );

    if (!users) {
      throw new Error("User profile not found!")
    }

    return users;
  }


  function keyVal(key: string, value: number | null, cond: number | null = value): string {
    if (cond) {
      return key + "=" + value + ","
    } else {
      return ""
    }
  }


  let filter = `?`
    + `${keyVal("sportId", sportId)}`
    + `${keyVal("cityId", cityId)}`
    + `${keyVal("ageMin", ageMin)}`
    + `${keyVal("ageMax",ageMax)}`
    + `${keyVal("skillLevelId", skillLevelId, sportId && skillLevelId)}`;

  if (filter.endsWith(',')) {
    filter = filter.slice(0, filter.length - 1)
  }

  // Ako je samo '?'
  if (filter.length === 1) {
    filter = ""
  }

  return fetchData<PublicUserInfo[]>(`/users${filter}`);
}


export async function getUserInfo(userId: number): Promise<UserDetails> {
  if (USE_DUMMY_DATA) {
    const user = dummyUsers.find(u => u.id === userId);

    if (!user) {
      throw new Error("User profile not found!")
    }

    return user;
  }

  return fetchData<UserDetails>(`/users/${userId}`);
}
