import {
  useQuery,
  useMutation,
  useQueryClient,
} from "@tanstack/react-query";

import * as api from "@/api/api_v1";

// TODO: Napraviti hooks za users
import type { CreateEventInfo } from "@/types/event";

export function useMyUserProfile() {
  return useQuery({
    queryKey: ["userProfile"],
    queryFn: api.getUserProfile,
  });
}

export function useUserProfile() {
  return useQuery({
    queryKey: ["myEvents"],
    queryFn: api.getMyEvents,
  });
}

export function useEventDetails(id: number) {
  return useQuery({
    queryKey: ["events", id],
    queryFn: () => api.getEventDetails(id),
  });
}

export function useCreateEvent() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (data: CreateEventInfo) => api.createEvent(data),

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
    mutationFn: (eventId: number) => api.joinEvent(eventId),

    onSuccess: () => {
      return Promise.all([
        queryClient.invalidateQueries({ queryKey: ["events"] }),
        queryClient.invalidateQueries({ queryKey: ["myEvents"] }),
      ]);
    },
  });
}

export function useCancelEvent() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (eventId: number) => api.cancelEvent(eventId),

    onSuccess: () => {
      return Promise.all([
        queryClient.invalidateQueries({ queryKey: ["events"] }),
        queryClient.invalidateQueries({ queryKey: ["myEvents"] }),
      ]);
    },
  });
}

export function useEventMembers(eventId: number) {
  return useQuery({
    queryKey: ["events", eventId, "members"],
    queryFn: () => api.getEventMembers(eventId),
  });
}
