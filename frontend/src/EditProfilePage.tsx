import { useEditUserProfile, useUserProfile, useIsAuth } from "@/apiStore/useUsers"
import { Avatar, AvatarImage } from "@/components/ui/avatar";
import { getDefaultAvatar } from "./lib/utils.ts";
import { Button } from "@/components/ui/button";
import { useState, useEffect } from "react";
import { Input } from "@/components/ui/input"
import {
    Select,
    SelectContent,
    SelectItem,
    SelectTrigger,
    SelectValue,
} from "@/components/ui/select"
import {
    Popover,
    PopoverContent,
    PopoverTrigger,
} from "@/components/ui/popover";
import { CalendarIcon, Plus, Trash2 } from "lucide-react";
import { Calendar } from "@/components/ui/calendar";

import { useCities, useSports, useSkillLevels } from "@/apiStore/useSif";
import { Label } from "@/components/ui/label"
import type { UserSportDetails } from "./types/user.ts";

export default function EditProfile() {
    const { data, isLoading, error } = useIsAuth();

    if (isLoading) {
        return <p>Provjera prijave...</p>;
    }

    if (error) {
        return <p>Došlo je do greške.</p>;
    }

    if (!data) {
        return <p>Korisnik nije prijavljen.</p>;
    }

    return (
        <>
            <div className="grid grid-cols-1 md:grid-cols-2 gap-x-16 gap-y-8 max-w-4xl mx-auto mt-20 px-4 items-start">

                <div className="flex flex-col gap-6">
                    <EditPicture />
                    <UserInfoInput />
                </div>

                <div className="">
                    <ChooseSport />
                </div>
            </div>
        </>
    );
}

export function EditPicture() {
    const { mutate: editUserProfile, isPending, error: editError } = useEditUserProfile();

    const { data: user, isLoading, error } = useUserProfile();

    if (isLoading) {
        return <p>Loading profile...</p>;
    }

    if (error) {
        return <p>Error loading profile</p>;
    }

    return (
        <>
            <div className="flex flex-col md:h-70 md:w-70 items-center">
                <Avatar className="md:h-60 md:w-60">
                    <AvatarImage
                        src={getDefaultAvatar(user.profilePhotoUrl) ?? ""}
                    />
                </Avatar>
                <Button className="mt-4 w-45">Uredi sliku</Button>
            </div>
        </>
    );

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
const GENDERS: Record<string, string> = {
    Male: "Muško",
    Female: "Žensko",
    Other: "Ne želim reći",
};
const TODAY = new Date();
const MIN_BIRTH_DATE = new Date(1920, 0, 1);


export function UserInfoInput() {
    const editUser = useEditUserProfile();

    const {
        data: user,
        isLoading,
        error,
    } = useUserProfile();

    const {
        data: CITIES,
        isLoading: citiesIsLoading,
        error: citiesError,
    } = useCities();


    const [name, setName] = useState("");
    const [username, setUsername] = useState("");
    const [gender, setGender] = useState<string | null>(null);
    const [dateOfBirth, setDateOfBirth] = useState<string | null>(null);
    const [cityId, setCityId] = useState<number | null>(null);

    const [serverError, setServerError] = useState<string | null>(null);

    const selectedCity = CITIES?.find(
        (city) => Number(city.id) === cityId
    );
    useEffect(() => {
        if (!user) return;

        setName(user.name);
        setUsername(user.username);
        setGender(user.gender);
        setDateOfBirth(user.dateOfBirth);
        setCityId(user.cityId);
    }, [user]);

    const handleSave = () => {
        setServerError(null);

        if (!name.trim() || !username.trim()) {
            setServerError("Ime i korisničko ime su obavezni.");
            return;
        }

        if (!gender || !dateOfBirth || !cityId) {
            setServerError("Molimo ispuni sva polja.");
            return;
        }

        if (!parseDate(dateOfBirth) || parseDate(dateOfBirth)! > TODAY) {
            setServerError("Unesi ispravan datum rođenja.");
            return;
        }

        editUser.mutate({
            name: name.trim(),
            username: username.trim(),
            gender,
            dateOfBirth,
            cityId,
            sports: user?.sports ?? [],
        });
    };

    if (isLoading || citiesIsLoading) {
        return <p>Loading profile...</p>;
    }

    if (error || !user) {
        return <p>Error loading profile.</p>;
    }

    if (citiesError) {
        return <p>Error loading cities.</p>;
    }

    return (
        <div className="space-y-6 mt-6">

            <div className="space-y-2 md:w-100">
                <Label htmlFor="name">Ime i prezime</Label>
                <Input
                    id="name"
                    value={name}
                    onChange={(e) => setName(e.target.value)}
                />
            </div>

            <div className="space-y-2 md:w-100">
                <Label htmlFor="username">Korisničko ime</Label>
                <Input
                    id="username"
                    value={username}
                    onChange={(e) => setUsername(e.target.value)}
                />
            </div>

            <div className="space-y-2">
                <Label>Spol</Label>

                <Select
                    value={gender ?? ""}
                    onValueChange={(value) => setGender(value)}
                >
                    <SelectTrigger>
                        <span>
                            {gender ? GENDERS[gender] : "Odaberi spol"}
                        </span>
                    </SelectTrigger>

                    <SelectContent>
                        <SelectItem value="Male">Muško</SelectItem>
                        <SelectItem value="Female">Žensko</SelectItem>
                        <SelectItem value="Other">Ne želim reći</SelectItem>
                    </SelectContent>
                </Select>
            </div>

            <div className="space-y-2">
                <Label>Datum rođenja</Label>

                <Popover>
                    <PopoverTrigger asChild>
                        <Button variant="outline" type="button">
                            <CalendarIcon className="mr-2 h-4 w-4" />
                            {parseDate(dateOfBirth)?.toLocaleDateString() ??
                                "Odaberi datum rođenja"}
                        </Button>
                    </PopoverTrigger>

                    <PopoverContent className="w-auto p-0">
                        <Calendar
                            mode="single"
                            selected={parseDate(dateOfBirth)}
                            onSelect={(date) =>
                                setDateOfBirth(date ? formatDate(date) : null)
                            }
                            disabled={(date) => date > TODAY}
                            captionLayout="dropdown"
                            startMonth={MIN_BIRTH_DATE}
                            endMonth={TODAY}
                            autoFocus
                        />
                    </PopoverContent>
                </Popover>
            </div>

            <div className="space-y-2">
                <Label>Grad</Label>

                <Select
                    value={cityId !== null ? String(cityId) : ""}
                    onValueChange={(value) => setCityId(Number(value))}
                >
                    <SelectTrigger>
                        <SelectValue placeholder="Odaberi grad">
                            {selectedCity ? selectedCity.name : "Odaberi grad"}
                        </SelectValue>
                    </SelectTrigger>

                    <SelectContent>
                        {CITIES?.map((city) => (
                            <SelectItem key={city.id} value={String(city.id)}>
                                {city.name}
                            </SelectItem>
                        ))}
                    </SelectContent>
                </Select>
            </div>

            {
                serverError && (
                    <p className="text-sm text-destructive">
                        {serverError}
                    </p>
                )
            }

            {
                editUser.error && (
                    <p className="text-sm text-destructive">
                        Došlo je do greške prilikom spremanja profila.
                    </p>
                )
            }

            <Button
                type="button"
                onClick={handleSave}
                disabled={editUser.isPending}
            >
                {editUser.isPending ? "Spremanje..." : "Spremi promjene"}
            </Button>

            {
                editUser.isSuccess && (
                    <p className="text-sm text-green-600">
                        Profil je uspješno ažuriran.
                    </p>
                )
            }
        </div >
    );


}

export function ChooseSport() {
    const {
        data: user,
        isLoading: userIsLoading,
        error: userError,
    } = useUserProfile();

    const {
        data: SPORTS,
        isLoading: sportsIsLoading,
        error: sportsError,
    } = useSports();

    const {
        data: SKILL_LEVELS,
        isLoading: skillsIsLoading,
        error: skillsError,
    } = useSkillLevels();

    const editUser = useEditUserProfile();

    const [sports, setSports] = useState<UserSportDetails[]>([]);
    const [isEditing, setIsEditing] = useState(false);
    const [saveError, setSaveError] = useState<string | null>(null);

    // Prikazujemo spremljene sportove ili privremene promjene
    const displayedSports = isEditing ? sports : user?.sports ?? [];

    // Početak uređivanja
    function startEditing() {
        setSports(user?.sports ?? []);
        setSaveError(null);
        editUser.reset();
        setIsEditing(true);
    }

    // Promjena razine znanja
    function updateSkillLevel(index: number, skillLevelId: number) {
        setSports((prev) =>
            prev.map((sport, i) =>
                i === index ? { ...sport, skillLevelId } : sport
            )
        );
    }

    // Odabir sporta
    function updateSport(index: number, sportId: number) {
        setSports((prev) =>
            prev.map((sport, i) =>
                i === index ? { ...sport, sportId } : sport
            )
        );
    }

    // Dodavanje sporta
    function addSport() {
        setSports((prev) => [
            ...prev,
            {
                sportId: 0,
                skillLevelId: 0,
            },
        ]);
    }

    // Brisanje sporta
    function deleteSport(index: number) {
        setSports((prev) => prev.filter((_, i) => i !== index));
    }

    // Odustajanje od promjena
    function cancelEditing() {
        setSaveError(null);
        editUser.reset();
        setIsEditing(false);
    }

    // Spremanje promjena
    function saveChanges() {
        if (!user) return;

        const sportIds = sports.map((sport) => sport.sportId);

        const hasInvalidSport = sports.some(
            (sport) => sport.sportId <= 0 || sport.skillLevelId <= 0
        );

        const hasDuplicates = new Set(sportIds).size !== sportIds.length;

        if (hasInvalidSport || hasDuplicates) {
            setSaveError("Provjeri odabrane sportove i njihove razine znanja.");
            return;
        }

        setSaveError(null);

        editUser.mutate(
            {
                username: user.username,
                name: user.name,
                gender: user.gender,
                dateOfBirth: user.dateOfBirth,
                cityId: user.cityId,
                sports,
            },
            {
                onSuccess: () => {
                    setIsEditing(false);
                    setSports([]);
                },
            }
        );
    }

    if (userIsLoading || sportsIsLoading || skillsIsLoading) {
        return <p>Učitavanje sportova...</p>;
    }

    if (
        userError ||
        sportsError ||
        skillsError ||
        !user ||
        !SPORTS ||
        !SKILL_LEVELS
    ) {
        return (
            <p className="text-sm text-destructive">
                Došlo je do pogreške prilikom učitavanja sportova.
            </p>
        );
    }

    const availableSports = SPORTS.filter(
        (sport) =>
            !sports.some((selected) => selected.sportId === sport.id)
    );

    const canSave =
        sports.every(
            (sport) => sport.sportId > 0 && sport.skillLevelId > 0
        ) &&
        new Set(sports.map((sport) => sport.sportId)).size ===
        sports.length;

    return (
        <div className="space-y-4">
            <div className="flex items-center justify-between">
                <h2 className="text-lg font-semibold">Moji sportovi</h2>

                {!isEditing && (
                    <Button
                        type="button"
                        variant="outline"
                        onClick={startEditing}
                    >
                        Uredi
                    </Button>
                )}
            </div>

            {/* Popis sportova */}
            <div className="space-y-3">
                {displayedSports.map((sport, index) => {
                    const sportName = SPORTS.find(
                        (s) => s.id === sport.sportId
                    )?.name;

                    const skillName = SKILL_LEVELS.find(
                        (level) => level.id === sport.skillLevelId
                    )?.name;

                    return (
                        <div
                            key={index}
                            className="flex items-center gap-3 rounded-lg border p-3 w-100"
                        >
                            {/* Ime sporta */}
                            {isEditing && sport.sportId === 0 ? (
                                <Select
                                    value=""
                                    onValueChange={(value) =>
                                        updateSport(index, Number(value))
                                    }
                                >
                                    <SelectTrigger className="flex-1">
                                        <SelectValue placeholder="Odaberi sport" />
                                    </SelectTrigger>

                                    <SelectContent>
                                        {availableSports.map((s) => (
                                            <SelectItem
                                                key={s.id}
                                                value={String(s.id)}
                                            >
                                                {s.name}
                                            </SelectItem>
                                        ))}
                                    </SelectContent>
                                </Select>
                            ) : (
                                <span className="flex-1 font-medium">
                                    {sportName ?? "Nepoznat sport"}
                                </span>
                            )}

                            {/* Razina znanja */}
                            {isEditing ? (
                                <Select
                                    value={
                                        sport.skillLevelId > 0
                                            ? String(sport.skillLevelId)
                                            : ""
                                    }
                                    onValueChange={(value) =>
                                        updateSkillLevel(index, Number(value))
                                    }
                                >
                                    <SelectTrigger className="flex-1">
                                        <SelectValue placeholder="Razina znanja">
                                            {skillName}
                                        </SelectValue>
                                    </SelectTrigger>

                                    <SelectContent>
                                        {SKILL_LEVELS.map((level) => (
                                            <SelectItem
                                                key={level.id}
                                                value={String(level.id)}
                                            >
                                                {level.name}
                                            </SelectItem>
                                        ))}
                                    </SelectContent>
                                </Select>
                            ) : (
                                <span className="text-sm text-muted-foreground">
                                    {skillName ?? "Nepoznata razina"}
                                </span>
                            )}

                            {/* Brisanje */}
                            {isEditing && (
                                <Button
                                    type="button"
                                    variant="ghost"
                                    size="icon"
                                    onClick={() => deleteSport(index)}
                                    aria-label="Izbriši sport"
                                >
                                    <Trash2 className="h-4 w-4" />
                                </Button>
                            )}
                        </div>
                    );
                })}

                {displayedSports.length === 0 && (
                    <p className="text-sm text-muted-foreground">
                        Još nema dodanih sportova.
                    </p>
                )}
            </div>

            {/* Opcije uređivanja */}
            {isEditing && (
                <div className="space-y-3 w-100">
                    <Button
                        type="button"
                        variant="outline"
                        className="w-full"
                        disabled={
                            availableSports.length === 0 ||
                            sports.some((sport) => sport.sportId === 0)
                        }
                        onClick={addSport}
                    >
                        <Plus className="mr-2 h-4 w-4" />
                        Dodaj sport
                    </Button>

                    {saveError && (
                        <p className="text-sm text-destructive">
                            {saveError}
                        </p>
                    )}

                    {editUser.error && (
                        <p className="text-sm text-destructive">
                            Došlo je do greške prilikom spremanja sportova.
                        </p>
                    )}

                    <div className="flex gap-3">
                        <Button
                            type="button"
                            variant="outline"
                            className="flex-1"
                            onClick={cancelEditing}
                            disabled={editUser.isPending}
                        >
                            Odustani
                        </Button>

                        <Button
                            type="button"
                            className="flex-1"
                            onClick={saveChanges}
                            disabled={!canSave || editUser.isPending}
                        >
                            {editUser.isPending ? "Spremanje..." : "Spremi"}
                        </Button>
                    </div>
                </div>
            )}
        </div>
    );
}
