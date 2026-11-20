import { useEditUserProfile, useUserProfile } from "@/apiStore/useUsers"
import { Avatar, AvatarFallback, AvatarImage } from "@/components/ui/avatar";
import { getDefaultAvatar } from "./lib/utils.ts";
import { Button } from "@/components/ui/button";
import { useState, useEffect } from "react";
import {
    Field,
    FieldDescription,
    FieldLabel,
} from "@/components/ui/field"
import { Input } from "@/components/ui/input"
import {
    Select,
    SelectContent,
    SelectGroup,
    SelectItem,
    SelectLabel,
    SelectTrigger,
    SelectValue,
} from "@/components/ui/select"
import {
    Popover,
    PopoverContent,
    PopoverTrigger,
} from "@/components/ui/popover";
import { CalendarIcon, Plus, Trash } from "lucide-react";
import { Calendar } from "@/components/ui/calendar";

import { useCities } from "@/apiStore/useSif";
import { Label } from "@/components/ui/label"

export default function EditProfile() {
    return (
        <>
            <div className="grid grid-cols-1 md:grid-cols-2 gap-6 w-lg mx-auto mt-10">
                
                <div className="flex flex-col gap-6">
                <EditPicture />
                <UserInfoInput />
                </div>

                <div></div>
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

