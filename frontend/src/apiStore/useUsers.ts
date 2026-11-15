import {
  useQuery,
  useMutation,
  useQueryClient,
} from "@tanstack/react-query";

import * as api from "@/api/api_v1";

import type { UserCreateDetails } from "@/types/user";

export function useUserProfile() {
  return useQuery({
    queryKey: ["userProfile"],
    queryFn: api.getUserProfile,
  });
}

export function useEditUserProfile() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (data: UserCreateDetails) => api.editUserProfile(data),

    onSuccess: () => {
      return Promise.all([
        queryClient.invalidateQueries({ queryKey: ["userProfile"] }),
        queryClient.invalidateQueries({ queryKey: ["users"] }),
      ]);
    },
  });
}

export function useUserLogout() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: () => api.logoutUser(),

    onSuccess: () => {
      return queryClient.clear();
    },
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
