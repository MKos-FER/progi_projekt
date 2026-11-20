import { dummyEvents, dummyUsers, currentUserId, preferedUserLocation } from "@/dummy_data/simple_dummy_data";
import type { EventCardInfo, CreateEventInfo, EventDetails, EventFilters } from "@/types/event";
import type { PublicUserInfo, UserDetails, UserCreateDetails } from "@/types/user";
import type { Sport } from "@/types/sport";
import type { City } from "@/types/city";
import { ApiError, type RegisterResult } from "@/types/auth";
import { calculateDistanceKm, getUserLocation, type UserLocation } from "@/lib/geolocation";
import { getDefaultAvatar } from "@/lib/utils";
import type { SkillLevel } from "@/types/skill-level";

// const USE_DUMMY_DATA = import.meta.env.APP_USE_DUMMY_DATA === "true";
const USE_DUMMY_DATA = false;

const USE_DUMMY_USERS = false;
const USE_DUMMY_EVENTS = false;

const API_PREFIX = `/api/v1`;

async function fetchData<T>(url: string, options?: RequestInit): Promise<T> {
  const response = await fetch(API_PREFIX + url, options);

  if (!response.ok) {
    throw new Error(`Request failed: ${response.status}`);
  }

  return (await response.json()) as T;
}

const getXsrf = () => document.cookie.match(/XSRF-TOKEN=([^;]+)/)?.[1] ?? '';

async function send(url: string, init: RequestInit = {}): Promise<Response> {
  const doSend = () => fetch(API_PREFIX + url, {
    ...init,
    credentials: 'include',
    headers: { ...init.headers, 'X-XSRF-TOKEN': getXsrf() },
  });
  let res = await doSend();

  // Znači da XSRF token nije bio dobro postavljen
  if (res.status === 403) {
    // Napravi token reload i ponovi request
    await fetch(API_PREFIX + '/auth/csrf', { credentials: 'include' });
    res = await doSend();
  }
  return res;
}

async function throwResposneMessage(response: Response): Promise<void> {
  if (!response.ok) {
    const r = (await response.json()) as { message: string }
    throw new Error(`${r.message}`);
  }
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
 * Sif api
 * /sports
 * /venues
 * /cities
 */

export async function getSports(): Promise<Sport[]> {
  return await fetchData<Sport[]>("/sports")
}

export async function getCities(): Promise<City[]> {
  return await fetchData<City[]>("/cities")
}

export async function getSkillLevels(): Promise<SkillLevel[]> {
  return await fetchData<SkillLevel[]>("/skill-levels")
}

/*
 *  Event api
 *  /event/*
 */

export async function getEvents(filters?: EventFilters): Promise<EventCardInfo[]> {
  if (USE_DUMMY_DATA && USE_DUMMY_EVENTS) {
    const userLoc = await getUserPreferredLocation();

    const events = dummyEvents.map(event => ({
      ...event,
      distance_km: roundSometimes(calculateDistanceKm(userLoc.latitude,
        userLoc.longitude, event.latitude, event.longitude)),
    })) satisfies EventCardInfo[];

    if (!filters) {
      return events;
    }

    return events.filter(event =>
      (!filters.cityId || filters.cityId.includes(event.cityId ?? -1))
      && (!filters.sportId || filters.sportId.includes(event.sportId ?? -1))
      && (!filters.distance_km || event.distance_km <= filters.distance_km)
    )
  }

  // http://localhost:8080/api/v1/events?cityIds=9,7&sportIds=6,7
  let cityIds = "cityIds="
  let sportIds = "sportIds="

  if (filters?.cityId) {
    cityIds += filters.cityId.join(",")
  }

  if (filters?.sportId) {
    sportIds += filters.sportId.join(",")
  }

  let events = await fetchData<EventCardInfo[]>(`/events?${cityIds}&${sportIds}`);
  const userLoc = await getUserPreferredLocation();

  events = events.map(event => ({
    ...event,
    distance_km: roundSometimes(calculateDistanceKm(userLoc.latitude,
      userLoc.longitude, event.latitude, event.longitude)),
  })) satisfies EventCardInfo[];

  if (filters) {
    events = events.filter(event =>
      !filters.distance_km || event.distance_km <= filters.distance_km
    )
  }

  const sports = await fetchData<Sport[]>(`/sports`);

  return events.map(event => ({
    ...event,
    sportName: (sports.find(s => s.id === event.sportId)?.name) ?? "Unknown sport",
  }))
}

export async function getMyEvents(): Promise<EventCardInfo[]> {
  if (USE_DUMMY_DATA && USE_DUMMY_EVENTS) {
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

  const events = await fetchData<EventCardInfo[]>("/users/me/events");
  const sports = await fetchData<Sport[]>(`/sports`);

  return events.map(event => ({
    ...event,
    sportName: (sports.find(s => s.id === event.sportId)?.name) ?? "Unknown sport",
  }))
}

export async function getEventDetails(id: number): Promise<EventDetails> {
  if (USE_DUMMY_DATA && USE_DUMMY_EVENTS) {
    const userLoc = await getUserPreferredLocation();

    const event: EventDetails = dummyEvents.find(el => el.id === id)!;
    event.distance_km = roundSometimes(calculateDistanceKm(userLoc.latitude,
      userLoc.longitude, event.latitude, event.longitude));

    return event;
  }

  const event = await fetchData<EventDetails>(`/events/${id}`);
  const sports = await fetchData<Sport[]>(`/sports`);

  return {
    ...event,
    sportName: (sports.find(s => s.id === event.sportId)?.name) ?? "Unknown sport",
  } satisfies EventDetails;
}

export async function createEvent(data: CreateEventInfo): Promise<EventCardInfo> {
  if (USE_DUMMY_DATA && USE_DUMMY_EVENTS) {
    const event: EventDetails = {
      ...data,
      id: Math.max(0, ...dummyEvents.map(e => e.id)) + 1,
      participants: [],
      status: null,
      createdAt: Date.now().toString(),
    };

    dummyEvents.push(event);

    return event;
  }

  const response = await send("/events", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    body: JSON.stringify(data),
  });

  await throwResposneMessage(response)
}

// TODO: Figure out edit
export async function editEvent(data: CreateEventInfo): Promise<EventCardInfo> {
  if (USE_DUMMY_DATA && USE_DUMMY_EVENTS) {
    throw new Error("NOT IMPLEMENTED");
    const event: EventDetails = {
      ...data,
      id: Math.max(0, ...dummyEvents.map(e => e.id)) + 1,
      participants: [],
      status: null,
      createdAt: Date.now().toString(),
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
  if (USE_DUMMY_DATA && USE_DUMMY_EVENTS) {
    const eventIdx = dummyEvents.findIndex(e => e.id === eventId);
    if (eventIdx === -1) {
      throw new Error("Event cannot be canceled as it does not exist!");
    }

    const event = dummyEvents[eventIdx];
    if (event.organizerId !== currentUserId) {
      throw new Error("Permission denied! You cannot cancel this event!");
    }

    dummyEvents.splice(eventIdx, 1);
    return;
  }

  const response = await send(`/events/${eventId}/cancel`, {
    method: "POST",
  });

  await throwResposneMessage(response)
}

export async function joinEvent(eventId: number): Promise<void> {
  if (USE_DUMMY_DATA && USE_DUMMY_EVENTS) {
    const event = dummyEvents.find(e => e.id === eventId);

    if (!event) throw new Error("Event not found");

    if (event.participants.includes(currentUserId)) {
      throw new Error("Already joined");
    }

    if (event.participants.length >= event.maxParticipants) {
      throw new Error("Event is full");
    }

    event.participants.push(currentUserId);

    return;
  }

  const response = await send(`/events/${eventId}/members`, {
    method: "POST",
  });

  await throwResposneMessage(response)
}

// TODO: Figure out leave
export async function leaveEvent(eventId: number): Promise<void> {
  if (USE_DUMMY_DATA && USE_DUMMY_EVENTS) {
    throw new Error("NOT IMPLEMENTED");
  }

  const response = await send(`/events/${eventId}/members/me`, {
    method: "DELETE",
  });

  await throwResposneMessage(response)
}

export async function getEventMembers(eventId: number): Promise<PublicUserInfo[]> {
  if (USE_DUMMY_DATA && USE_DUMMY_EVENTS) {
    const event = dummyEvents.find(e => e.id === eventId);

    if (!event) throw new Error("Event not found");

    return event.participants.map(userId => {
      const user = dummyUsers.find(u => u.userId === userId);
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
  if (USE_DUMMY_DATA && USE_DUMMY_USERS) {
    const user = dummyUsers.find(u => u.userId === currentUserId);

    if (!user) {
      throw new Error("User profile not found!")
    }

    return user;
  }

  return fetchData<UserDetails>(`/users/me`);
}


export async function editUserProfile(newUserInfo: UserCreateDetails): Promise<void> {
  if (USE_DUMMY_DATA && USE_DUMMY_USERS) {
    const userIdx = dummyUsers.findIndex(u => u.userId === currentUserId);

    if (userIdx === -1) {
      throw new Error("User profile not found!")
    }

    dummyUsers[userIdx] = {
      ...dummyUsers[userIdx],
      ...newUserInfo,
    };

    return;
  }


  const response = await send(`/users/me`, {
    method: "PATCH",
    headers: {
      "Content-Type": "application/json",
    },
    body: JSON.stringify(newUserInfo)
  });

  if (!response.ok) {
    throw new Error(`Cannot edit user profile: ${response.status}`)
  }
  // await throwResposneMessage(response)
}

export async function uploadUserProfilePhoto(file: FormData): Promise<void> {
  const response = await send(`/users/me/photo`, {
    method: "PUT",
    body: file
  });

  await throwResposneMessage(response)
}

export async function getUsers(sportId: number | null, cityId: number | null, ageMin: number | null, ageMax: number | null, skillLevelId: number | null): Promise<PublicUserInfo[]> {
  if (USE_DUMMY_DATA && USE_DUMMY_USERS) {
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
      (!sportId || u.sports.map(s => s.sportId).includes(sportId))
      && (!cityId || u.cityId === cityId)
      && (!ageMin || u.dateOfBirth && calculateAge(u.dateOfBirth) >= ageMin)
      && (!ageMax || u.dateOfBirth && calculateAge(u.dateOfBirth) <= ageMax)
      && ((!sportId || !skillLevelId) || u.sports.filter(s => s.sportId === sportId && s.skillLevelId >= skillLevelId).length > 0)
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
    + `${keyVal("ageMax", ageMax)}`
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
  if (USE_DUMMY_DATA && USE_DUMMY_USERS) {
    const user = dummyUsers.find(u => u.userId === userId);

    if (!user) {
      throw new Error("User profile not found!")
    }

    return user;
  }

  return fetchData<UserDetails>(`/users/${userId}`);
}

// TODO: Error checking
export async function logoutUser(): Promise<void> {
  await send('/auth/logout', {
    method: 'POST',
  });
}


export async function registerUser(user: UserCreateDetails): Promise<void> {

  user.sports = user.sports.map(s => { return { sportId: s.sportId, skillLevelId: s.skillLevelId } });

  const res = await send('/auth/register', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(user),
  });

  if (res.status === 201) {
    return;
  }

  const body = await res.json().catch(() => { }) as RegisterResult;

  throw new ApiError(
    res.status,
    body.errors,
    body.message
  );
}

export async function getUserIsAuth(): Promise<boolean> {
  const result = await fetch("/api/v1/users/me");

  if (result.status === 401) {
    return false;
  } else {
    return true;
  }
}
