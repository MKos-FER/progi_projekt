export type AppRole = "GUEST" | "USER" | "MODERATOR";

export interface UserSportDetails {
  id: number;
  sportId: number;
  sportName: string;
  skillLevelId: number;
}

export interface UserDetails {
  userId: number;
  username: string;
  name: string;
  profilePhotoUrl: string | null;
  gender: string | null;
  dateOfBirth: string | null;
  city_id: number | null;
  cityName: string | null;
  appRoleId: AppRole;
  userSports: UserSportDetails[];
  joinedAt: string
}

export type UserCreateDetails = Omit<UserDetails, "userId | profilePhotoUrl | cityName | appRoleId">


export interface PublicUserInfo {
    userId: number;
    username: string;
    name: string;
    profilePhotoUrl: string | null;
    userSports: UserSportDetails[];
    appRoleId: AppRole;
    joinedAt: string
}
