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
      return Promise.all([
        queryClient.invalidateQueries({ queryKey: ["userProfile"] }),
        queryClient.invalidateQueries({ queryKey: ["isAuth"] })
      ]);
    },
  });
}

export function useCreateUser() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (data: UserCreateDetails) => api.registerUser(data),

    onSuccess: () => {
      return Promise.all([
        queryClient.invalidateQueries({ queryKey: ["userProfile"] }),
      ]);
    },
  });
}

export function useIsAuth() {
  return useQuery({
    queryKey: ["isAuth"],
    queryFn: api.getUserIsAuth,
  });
}

export function useUploadUserProfilePhoto() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (data: FormData) => api.uploadUserProfilePhoto(data),

    onSuccess: () => {
      return Promise.all([
        queryClient.invalidateQueries({ queryKey: ["userProfile"] }),
        queryClient.invalidateQueries({ queryKey: ["users"] }),
      ]);
    },
  });
}
