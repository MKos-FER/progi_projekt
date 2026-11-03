import {
  useQuery,
  useMutation,
  useQueryClient,
} from "@tanstack/react-query";

import {
  getEvents,
  getMyEvents,
  getEventDetails,
  createEvent,
  joinEvent,
} from "@/api/events";

import type { CreateEventInfo } from "@/types/event.ts";

// get all events
export function useEvents() {
  return useQuery({
    queryKey: ["events"],
    queryFn: getEvents,
  });
}

export function useMyEvents() {
  return useQuery({
    queryKey: ["myEvents"],
    queryFn: getMyEvents,
  });
}

export function useEventDetails(id: number) {
  return useQuery({
    queryKey: ["events", id],
    queryFn: () => getEventDetails(id),
  });
}

export function useCreateEvent() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (data: CreateEventInfo) => createEvent(data),

    onSuccess: () => {
      return Promise.all([
        queryClient.invalidateQueries({ queryKey: ["events"] }),
        queryClient.invalidateQueries({ queryKey: ["myEvents"] }),
      ]);
    },
  });
}

export function useJoinEvent() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (eventId: number) => joinEvent(eventId),

    onSuccess: () => {
      return Promise.all([
        queryClient.invalidateQueries({ queryKey: ["events"] }),
        queryClient.invalidateQueries({ queryKey: ["myEvents"] }),
      ]);
    },
  });
}