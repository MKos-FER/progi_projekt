export type AppRole = "GUEST" | "USER" | "MODERATOR";

export interface UserSportDetails {
  id: number;
  sport_id: number;
  sportName: string;
  skill_level_id: number;
}

export interface UserDetails {
  id: number;
  username: string;
  name: string;
  profile_photo_url: string | null;
  gender: string | null;
  date_of_birth: string | null;
  city_id: number | null;
  cityName: string | null;
  app_role_id: AppRole;
  created_at: string | null;
  user_sports: UserSportDetails[];
}


export interface PublicUserInfo {
    id: number;
    username: string;
    name: string;
    profile_photo_url: string | null;
    user_sports: UserSportDetails[];
    app_role_id: AppRole;
}
