export type AppRole = "GUEST" | "USER" | "MODERATOR";

export interface UserSportDetails {
  sportId: number;
  sportName?: string;
  skillLevelId: number;
  skillLevelName?: number;
}

export interface UserDetails {
  userId: number;
  username: string;
  name: string;
  profilePhotoUrl: string | null;
  gender: string | null;
  dateOfBirth: string | null;
  cityId: number | null;
  cityName: string | null;
  appRoleId: AppRole;
  sports: UserSportDetails[];
  joinedAt: string
}

export type UserCreateDetails = Omit<UserDetails, "userId" | "profilePhotoUrl" | "cityName" | "appRoleId" | "joinedAt">


export interface PublicUserInfo {
    userId: number;
    username: string;
    name: string;
    profilePhotoUrl: string | null;
    sports: UserSportDetails[];
    appRoleId: AppRole;
    joinedAt: string
}
