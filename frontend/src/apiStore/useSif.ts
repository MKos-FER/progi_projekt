import {
  useQuery,
} from "@tanstack/react-query";

import * as api from "@/api/api_v1";

export function useSports() {
  return useQuery({
    queryKey: ["sports"],
    queryFn: api.getSports,
  });
}

export function useCities() {
  return useQuery({
    queryKey: ["cities"],
    queryFn: api.getCities,
  });
}

export function useSkillLevels() {
  return useQuery({
    queryKey: ["skillLevels"],
    queryFn: api.getSkillLevels,
  });
}
