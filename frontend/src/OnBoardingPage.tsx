import { useState, type SyntheticEvent } from "react";

import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Calendar } from "@/components/ui/calendar";
import {
  Popover,
  PopoverContent,
  PopoverTrigger,
} from "@/components/ui/popover";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";

import { CalendarIcon, Plus, Trash } from "lucide-react";

import { useCreateUser } from "@/apiStore/useUsers";
import { ApiError } from "@/types/auth";

import type { UserCreateDetails, UserSportDetails } from "@/types/user";

const SPORTS = [
  { id: 1, name: "Football" },
  { id: 2, name: "Basketball" },
  { id: 3, name: "Tennis" },
  { id: 4, name: "Volleyball" },
];

const SKILL_LEVELS = [
  { id: 1, name: "Beginner" },
  { id: 2, name: "Intermediate" },
  { id: 3, name: "Advanced" },
  { id: 4, name: "Expert" },
];

type FormErrors = Partial<Record<keyof UserCreateDetails, string>>;

function createEmptySport(): UserSportDetails {
  return {
    id: 0,
    sportId: 0,
    sportName: "",
    skillLevelId: 0,
  };
}

function parseDate(value: string | null): Date | undefined {
  if (!value) return undefined;

  const [year, month, day] = value.split("-").map(Number);
  const date = new Date(year, month - 1, day);

  return date.getFullYear() === year &&
    date.getMonth() === month - 1 &&
    date.getDate() === day
    ? date
    : undefined;
}

function formatDate(date: Date): string {
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, "0");
  const day = String(date.getDate()).padStart(2, "0");

  return `${year}-${month}-${day}`;
}

const TODAY = new Date();
const MIN_BIRTH_DATE = new Date(1920, 0, 1);

export default function RegistrationForm() {
  const createUser = useCreateUser();

  const [user, setUser] = useState<UserCreateDetails>({
    username: "",
    name: "",
    gender: null,
    dateOfBirth: null,
    cityId: null,
    sports: [],
  });

  const [errors, setErrors] = useState<FormErrors>({});
  const [serverError, setServerError] = useState<string | null>(null);

  function update<K extends keyof UserCreateDetails>(
    key: K,
    value: UserCreateDetails[K]
  ) {
    setUser((prev) => ({ ...prev, [key]: value }));

    setErrors((prev) => {
      const next = { ...prev };
      delete next[key];
      return next;
    });
  }

  function validate(): boolean {
    const newErrors: FormErrors = {};

    if (!user.username.trim()) {
      newErrors.username = "Korisničko ime je obavezno.";
    } else if (user.username.trim().length < 3) {
      newErrors.username = "Korisničko ime mora imati barem 3 znaka.";
    } else if (user.username.trim().length > 30) {
      newErrors.username = "Korisničko ime ne može biti duže od 30 znakova.";
    }

    if (!user.name.trim()) {
      newErrors.name = "Ime je obavezno.";
    } else if (user.name.trim().length < 2) {
      newErrors.name = "Ime mora imati barem 2 znaka.";
    } else if (user.name.trim().length > 100) {
      newErrors.name = "Ime ne može biti duže od 100 znakova.";
    }

    if (
      user.gender !== null &&
      !["Male", "Female", "Other"].includes(user.gender)
    ) {
      newErrors.gender = "Spoj nije dobro unesen.";
    }

    if (!user.dateOfBirth) {
      newErrors.dateOfBirth = "Datum rođenja je obavezan!";
    } else {
      const date = parseDate(user.dateOfBirth);
      const today = formatDate(new Date());

      if (!date) {
        newErrors.dateOfBirth = "Datum nije ispravno unesen.";
      } else if (user.dateOfBirth > today) {
        newErrors.dateOfBirth = "Datum rođenja ne može biti u budućnosti.";
      }
    }

    if (
      user.cityId !== null &&
      (!Number.isInteger(user.cityId) || user.cityId <= 0)
    ) {
      newErrors.cityId = "Grad nije ispravan.";
    }

    if (!user.sports.length) {
      newErrors.sports = "Odaberi barem jedan sport.";
    } else {
      const sportIds = user.sports.map((sport) => sport.sportId);

      if (sportIds.some((id) => !Number.isInteger(id) || id <= 0)) {
        newErrors.sports = "Odaberi ispravan sport.";
      } else if (new Set(sportIds).size !== sportIds.length) {
        newErrors.sports = "Ne može se odabrati isti sport dva puta.";
      } else if (
        user.sports.some((sport) =>
          !Number.isInteger(sport.skillLevelId) ||
          sport.skillLevelId <= 0
        )
      ) {
        newErrors.sports = "Odredi razinu iskustva za svaki sport.";
      }
    }

    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  }

  async function onSubmit(e: SyntheticEvent<HTMLFormElement>) {
    e.preventDefault();
    setServerError(null);

    if (!validate()) return;

    try {
      await createUser.mutateAsync({
        ...user,
        username: user.username.trim(),
        name: user.name.trim(),
      });

      window.location.href = "/";
    } catch (error) {
      if (error instanceof ApiError) {
        if (error.status === 400) {
          setErrors((prev) => ({ ...prev, ...error.errors }));
        } else {
          setServerError(error.message);
        }
      } else {
        setServerError("Došlo je do pogreške na našoj strani, pokušajte ponovno.");
      }
    }
  }

  return (
    <form onSubmit={(e) => void onSubmit(e)} className="mx-auto w-full max-w-md space-y-4">
      {serverError && (
        <p className="text-sm text-destructive">{serverError}</p>
      )}

      {/* Username */}
      <div className="space-y-1">
        <Label htmlFor="username">Korisničko ime</Label>
        <Input
          id="username"
          value={user.username}
          onChange={(e) => update("username", e.target.value)}
          autoComplete="username"
          aria-invalid={!!errors.username}
        />
        {errors.username && (
          <p className="text-sm text-destructive">{errors.username}</p>
        )}
      </div>

      {/* Name */}
      <div className="space-y-1">
        <Label htmlFor="name">Ime</Label>
        <Input
          id="name"
          value={user.name}
          onChange={(e) => update("name", e.target.value)}
          autoComplete="name"
          aria-invalid={!!errors.name}
        />
        {errors.name && (
          <p className="text-sm text-destructive">{errors.name}</p>
        )}
      </div>

      {/* Gender */}
      <div className="space-y-1">
        <Label>Spol</Label>
        <Select
          value={user.gender ?? "NONE"}
          onValueChange={(value) =>
            update("gender", value === "NONE" ? null : value)
          }
        >
          <SelectTrigger>
            <SelectValue placeholder="Odaberi spol" />
          </SelectTrigger>
          <SelectContent>
            <SelectItem value="NONE">Odaberi spol</SelectItem>
            <SelectItem value="Male">Muško</SelectItem>
            <SelectItem value="Female">Žensko</SelectItem>
            <SelectItem value="Other">Ne želim reći</SelectItem>
          </SelectContent>
        </Select>
        {errors.gender && (
          <p className="text-sm text-destructive">{errors.gender}</p>
        )}
      </div>

      {/* Date of birth */}
      <div className="space-y-1">
        <Label>Datum rođenja</Label>
        <Popover>
          <PopoverTrigger
            type="button"
            className="inline-flex h-9 w-full items-center justify-start rounded-lg border border-input bg-background px-3 py-2 text-sm font-normal shadow-xs hover:bg-accent hover:text-accent-foreground"
            aria-invalid={!!errors.dateOfBirth}
          >
            <CalendarIcon className="mr-2 h-4 w-4" />
            {parseDate(user.dateOfBirth)?.toLocaleDateString() ?? "Odaberi datum rođenja"}
          </PopoverTrigger>

          <PopoverContent className="w-auto p-0" align="start">
            <Calendar
              mode="single"
              selected={parseDate(user.dateOfBirth)}
              onSelect={(date) => update("dateOfBirth", date ? formatDate(date) : null)}
              disabled={(date) => date > new Date()}
              captionLayout="dropdown"
              startMonth={MIN_BIRTH_DATE}
              endMonth={TODAY}
              autoFocus
            />
          </PopoverContent>
        </Popover>
        {errors.dateOfBirth && (
          <p className="text-sm text-destructive">{errors.dateOfBirth}</p>
        )}
      </div>

      {/* City */}
      <div className="space-y-1">
        <Label htmlFor="cityId">CityId</Label>
        <Input
          id="cityId"
          type="number"
          min={1}
          value={user.cityId ?? ""}
          onChange={(e) =>
            update("cityId", e.target.value === "" ? null : Number(e.target.value))
          }
          aria-invalid={!!errors.cityId}
        />
        {errors.cityId && (
          <p className="text-sm text-destructive">{errors.cityId}</p>
        )}
      </div>

      {/* Sports */}
      <div className="space-y-2">
        <Label>Sportovi</Label>

        {user.sports.map((sport, index) => (
          <div key={sport.sportId} className="flex gap-2">
            <Select
              value={sport.sportId > 0 ? String(sport.sportId) : ""}
              onValueChange={(value) => {
                const selectedSport = SPORTS.find(
                  (s) => s.id === Number(value)
                );

                if (!selectedSport) return;

                const updated = [...user.sports];
                updated[index] = {
                  ...sport,
                  sportId: selectedSport.id,
                  sportName: selectedSport.name,
                };

                update("sports", updated);
              }}
            >
              <SelectTrigger>
                <SelectValue placeholder="Odaberi sport" />
              </SelectTrigger>
              <SelectContent>
                {SPORTS.filter((s) =>
                  !user.sports.some(
                    (selected, i) =>
                      i !== index && selected.sportId === s.id
                  )
                ).map((s) => (
                  <SelectItem key={s.id} value={String(s.id)}>
                    {s.name}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>

            <Select
              value={sport.skillLevelId > 0 ? String(sport.skillLevelId) : ""}
              onValueChange={(value) => {
                const updated = [...user.sports];
                updated[index] = {
                  ...sport,
                  skillLevelId: Number(value),
                };

                update("sports", updated);
              }}
            >
              <SelectTrigger>
                <SelectValue placeholder="Odredi razinu iskustva" />
              </SelectTrigger>
              <SelectContent>
                {SKILL_LEVELS.map((level) => (
                  <SelectItem key={level.id} value={String(level.id)}>
                    {level.name}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>

            <Button
              type="button"
              variant="outline"
              onClick={() =>
                update(
                  "sports",
                  user.sports.filter((_, i) => i !== index)
                )
              }
            >
              <Trash/> Izbriši
            </Button>
          </div>
        ))}

        {errors.sports && (
          <p className="text-sm text-destructive">{errors.sports}</p>
        )}

        <Button
          type="button"
          variant="outline"
          disabled={user.sports.length >= SPORTS.length}
          onClick={() =>
            update("sports", [...user.sports, createEmptySport()])
          }
        >
          <Plus />
          Dodaj sport
        </Button>
      </div>

      {/* Submit */}
      <Button type="submit" className="w-full" disabled={createUser.isPending}>
        {createUser.isPending ? "Kreiranje..." : "Kreiraj Profil"}
      </Button>
    </form>
  );
}
