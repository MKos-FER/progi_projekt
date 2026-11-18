import { useEvents, useMyEvents } from "@/apiStore/useEvents.ts";
import type { EventCardInfo, EventFilters } from "@/types/event.ts"
import { Skeleton } from "@/components/ui/skeleton"
import { useEffect, useState, type JSX } from "react";

import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import {
  MapPin,
  Users,
  Filter, CircleX,
  InfoIcon,
} from "lucide-react";
import { useNavigate } from "react-router-dom";
import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert.tsx";
import { Separator } from "@/components/ui/separator.tsx";
import { Item, ItemActions, ItemContent } from "@/components/ui/item.tsx";

import {
  Select,
  SelectContent,
  SelectGroup,
  SelectItem,
  SelectLabel,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select"
import { Label } from "@/components/ui/label"
import { Slider } from "@/components/ui/slider"
import { useCities, useSports } from "./apiStore/useSif";
import { useIsAuth } from "./apiStore/useUsers";

export default function MyEvents() {
  const { data: events, isLoading, error } = useMyEvents();
  const { data: isAuth } = useIsAuth();
  const navigate = useNavigate();

  let displayEvents: JSX.Element[] = [];

  if (!isAuth) {
    return (
      <div className="my-4 mx-auto flex flex-col gap-6">
        <Alert>
          <InfoIcon />
          <AlertTitle>Niste prijavljeni u aplikaciju</AlertTitle>
          <AlertDescription>
            Kada se prijavite, ovjde će se prikazati događaji na kojima sudjelujete.
          </AlertDescription>
        </Alert>
        <Item key="alert" variant="outline" className="w-1/3 min-w-100 self-center">
          <ItemContent>
            Trenutno nisi član niti jednog događaja
          </ItemContent>
          <ItemActions>
            <Button onClick={() => void navigate("/")}>
              Pronađi događaj
            </Button>
          </ItemActions>
        </Item>
      </div>
    )
  }

  if (error) {
    return (
      <div className="my-4 mx-auto flex flex-col gap-8">
        <Alert className="text-destructive *:[svg]:text-current bg-destructive-foreground">
          <CircleX />
          <AlertTitle>Dogodila se greška prilikom dohvata novih događaja</AlertTitle>
          <AlertDescription>
            Prilikom dohvata novih događaja u vašoj okolini došlo je do pogreške na serverskoj strani,
            ostanite strpljivi i pokušajte ponovno kasnije.
            <Separator className={"my-3"} />
            Error info: {error.message}
          </AlertDescription>
        </Alert>
      </div>
    );
  }

  if (events) {
    displayEvents = events.map(event => (
      <EventCard key={event.id} eventId={event.id} />
    ))
  }

  if (isLoading) {
    displayEvents.push(<SkeletonEventCard key={0} />);
    displayEvents.push(<SkeletonEventCard key={1} />);
    displayEvents.push(<SkeletonEventCard key={2} />);
  } else if (!events || displayEvents.length === 0) {
    displayEvents = [
      <Item key="alert" variant="outline" className="w-1/3 min-w-100 self-center">
        <ItemContent>
          Trenutno nisi član niti jednog događaja
        </ItemContent>
        <ItemActions>
          <Button onClick={() => void navigate("/")}>
            Pronađi događaj
          </Button>
        </ItemActions>
      </Item>
    ]
  }



  return (
    <div className="my-4 mx-auto flex flex-col gap-6">
      {displayEvents}
    </div>
  );
}


export function Filters({ filters, setFilters }: { filters?: EventFilters, setFilters: React.Dispatch<React.SetStateAction<EventFilters>> }) {
  const [open, setOpen] = useState(false);
  const [selectedCity, setSelectedCity] = useState<number | null>(null)
  const [selectedSport, setSelectedSport] = useState<number | null>(null)
  const [distanceKm, setDistanceKm] = useState<number | null>(null)

  useEffect(() => {
    if (!filters) {
      setDistanceKm(0)
    }
  }, [filters])

  // TODO: Error checking
  const { data: cities } = useCities();
  const { data: sports } = useSports();

  if (!cities) { return }
  if (!sports) { return }

  const selectableCities = cities.filter(c => !filters || !filters?.cityId?.includes(c.id)).map(c => { return { label: c.name, value: c.id }; })
  const selectableSports = sports.filter(s => !filters || !filters?.sportId?.includes(s.id)).map(s => { return { label: s.name, value: s.id }; })


  function setCityFilter(cityId: number | null) {
    setSelectedCity(null);

    setFilters((prev) => ({
      ...prev,
      cityId: prev?.cityId?.includes(cityId)
        ? prev.cityId ?? []
        : [...prev?.cityId ?? [], cityId],
    }));
  }

  function unsetCityFilter(cityId: number | null) {
    if (!filters?.cityId) {
      return;
    }

    setFilters((prev) => {
      if (!prev) return null;

      const updatedCities = prev.cityId?.filter(
        (id) => id !== cityId
      ) ?? [];

      const updatedFilters = {
        ...prev,
        cityId: updatedCities.length > 0 ? updatedCities : null,
      };

      const hasActiveFilters =
        updatedFilters.cityId ||
        updatedFilters.scheduledAt ||
        updatedFilters.endingBefore ||
        updatedFilters.sportId ||
        updatedFilters.distance_km;

      return hasActiveFilters ? updatedFilters : null;
    });
  }

  function setSportFilter(sportId: number) {
    setSelectedSport(null);

    setFilters((prev) => ({
      ...prev,
      sportId: prev?.sportId?.includes(sportId)
        ? prev.sportId ?? []
        : [...prev?.sportId ?? [], sportId],
    }));
  }

  function unsetSportFilter(sportId: number) {
    if (!filters?.sportId) {
      return;
    }

    setFilters((prev) => {
      if (!prev) return null;

      const updatedSports = prev.sportId?.filter(
        (id) => id !== sportId
      ) ?? [];

      const updatedFilters = {
        ...prev,
        sportId: updatedSports.length > 0 ? updatedSports : null,
      };

      const hasActiveFilters =
        updatedFilters.cityId ||
        updatedFilters.scheduledAt ||
        updatedFilters.endingBefore ||
        updatedFilters.sportId ||
        updatedFilters.distance_km;

      return hasActiveFilters ? updatedFilters : null;
    });
  }

  function setDistanceFilter(distanceKm: number) {
    setDistanceKm(distanceKm)

    if (distanceKm === 0) {
      setFilters((prev) => {
        if (!prev) return null;

        const updatedFilters = {
          ...prev,
          distance_km: null,
        };

        const hasActiveFilters =
          updatedFilters.cityId ||
          updatedFilters.scheduledAt ||
          updatedFilters.endingBefore ||
          updatedFilters.sportId ||
          updatedFilters.distance_km;

        return hasActiveFilters ? updatedFilters : null;
      });

      return
    }

    setFilters((prev) => ({
      ...prev,
      distance_km: distanceKm
    }));
  }

  return (
    <section className="space-y-3">

      <Button
        variant="outline"
        onClick={() => setOpen(!open)}
        aria-expanded={open}
      >
        <Filter className="mr-2 h-4 w-4" />
        FILTERI
      </Button>

      <Card className={`overflow-hidden animate-in fade-in slide-in-from-top-2 duration-120
                        ease-in-out ${open ? "max-h-45 opacity-100" : "max-h-0 opacity-0 m-0 p-0"}`}>
        <CardContent className="p-4">
          <div className="text-sm text-muted-foreground flex flex-row gap-2">
            <Select
              value={selectedCity}
              onValueChange={setCityFilter}
              items={selectableCities}
            >
              <SelectTrigger className="w-full max-w-48">
                <SelectValue placeholder="Odaberi grad" />
              </SelectTrigger>
              <SelectContent>
                <SelectGroup>
                  <SelectLabel>Gradovi</SelectLabel>
                  {selectableCities.map((item) => (
                    <SelectItem key={item.label} value={item.value}>
                      {item.label}
                    </SelectItem>
                  ))}
                </SelectGroup>
              </SelectContent>
            </Select>

            <Select
              value={selectedSport}
              onValueChange={setSportFilter}
              items={selectableSports}
            >
              <SelectTrigger className="w-full max-w-48">
                <SelectValue placeholder="Odaberi sport" />
              </SelectTrigger>
              <SelectContent>
                <SelectGroup>
                  <SelectLabel>Sportovi</SelectLabel>
                  {selectableSports.map((item) => (
                    <SelectItem key={item.label} value={item.value}>
                      {item.label}
                    </SelectItem>
                  ))}
                </SelectGroup>
              </SelectContent>
            </Select>

            <div className="mx-auto grid w-full max-w-xs gap-3">
              <div className="flex items-center justify-between gap-2">
                <Label htmlFor="slider-filter-distance-km">Udaljenost manja od</Label>
                <span className="text-sm text-muted-foreground">
                  {!distanceKm && "beskonačno" || distanceKm} km
                </span>
              </div>
              <Slider
                id="slider-filter-distance-km"
                value={!distanceKm ? 0 : distanceKm}
                onValueChange={(value) => {
                  if (typeof value === "number") {
                    setDistanceKm(value);
                  }
                }}
                onValueCommitted={(value) => setDistanceFilter(value as number)}
                min={0}
                max={10}
                step={0.1}
              />
            </div>
          </div>
        </CardContent>
      </Card>

      <Card className={`overflow-hidden animate-in fade-in slide-in-from-top-2 duration-120
                        ease-in-out ${filters ? "max-h-45 opacity-100" : "max-h-0 opacity-0 m-0 p-0"}`}>
        <CardContent className="flex min-h-10 flex-wrap items-center gap-2 p-3">
          <span className="text-sm text-muted-foreground">
            Odabrani filteri:
          </span>

          {filters && filters.cityId && filters.cityId.map(c => {
            return (
              <Badge className="text-md p-3" key={c} variant="secondary">
                {cities.find(ci => ci.id === c)?.name ?? "Unknown City"}
                <Button className="rounded-full p-0.5 pr-1 pl-1 m-0" variant="secondary" onClick={() => unsetCityFilter(c)}>
                  <CircleX className="size-4" />
                </Button>
              </Badge>
            );
          })
          }

          {filters && filters.sportId && filters.sportId.map(s => {
            return (
              <Badge className="text-md p-3" key={s} variant="secondary">
                {sports.find(sp => sp.id === s)?.name ?? "Unknown Sport"}
                <Button className="rounded-full p-0.5 pr-1 pl-1 m-0" variant="secondary" onClick={() => unsetSportFilter(s)}>
                  <CircleX className="size-4" />
                </Button>
              </Badge>
            );
          })
          }

          {distanceKm != null && distanceKm > 0 &&
            <Badge className="text-md p-3" variant="secondary">
              Udaljeno najviše {distanceKm} km
              <Button className="rounded-full p-0.5 pr-1 pl-1 m-0" variant="secondary" onClick={() => setDistanceFilter(0)}>
                <CircleX className="size-4" />
              </Button>
            </Badge>
          }
        </CardContent>
      </Card>

    </section>
  );
}

function EventImage() {
  return (
    <div className="flex min-h-32 w-full items-center justify-center
                    rounded-md border bg-muted md:w-48 md:shrink-0">
      <span className="text-sm text-muted-foreground">
        slika eventa
      </span>
    </div>
  );
}

export function SkeletonEventCard() {
  return (
    <Card>
      <CardContent className="flex flex-col gap-3 p-3 md:flex-row">

        <Skeleton className="flex min-h-32 w-full items-center justify-center
                    rounded-md border bg-muted md:w-48 md:shrink-0"/>

        <div className="
          grid
          w-full
          grid-cols-[1fr_1fr]
          md:grid-cols-[3fr_1fr]
          grid-rows-[auto_auto_auto]
          [grid-template-areas:'sport_loc'_'name_dist'_'desc_desc']
          md:[grid-template-areas:'sport_loc'_'name_loc'_'desc_dist']
          min-w-0
          gap-3
        ">

          <div className="flex flex-wrap items-center gap-2 [grid-area:sport]">

            <Badge variant="secondary">
              <Skeleton className="w-22" />
            </Badge>

            <Badge variant="outline">
              <Skeleton className="w-12" />
            </Badge>

          </div>

          <div className="ml-auto [grid-area:loc] w-full">
            <div className="flex flex-nowrap items-center gap-2 h-full justify-center">
              <Skeleton className="w-30 h-5" />
            </div>
          </div>

          <Skeleton className="text-center text-2xl md:text-5xl md:font-thin [grid-area:dist]">
          </Skeleton>

          <Skeleton className="text-lg md:text-2xl font-semibold [grid-area:name]" />

          <Skeleton className="rounded-md bg-muted p-3 text-sm text-muted-foreground [grid-area:desc]" />

        </div>
      </CardContent>
    </Card>
  )
}

export function EventCard({ eventId }: { eventId: number }) {
  const { data: events, isLoading, error } = useEvents();
  const navigate = useNavigate();

  if (error) {
    return (
      <>
        <div className="text-destructive">
          ERROR: {error.message}
        </div>
      </>
    );
  }

  if (isLoading || events!.findIndex(e => e.id === eventId) === -1) {
    return (
      <SkeletonEventCard />
    )
  }

  const event: EventCardInfo = events!.find(e => e.id == eventId)!
  return (
    // <Link to={`/events/${eventId}`} className="select-auto">
    <Card className="transition-all duration-200 hover:scale-101 active:scale-99 p-0"
      onClick={() => { // Ovako je cijeli Card i dalje clickable a text se i dalje može selectat
        if (window.getSelection()?.toString()) return;
        void navigate(`/events/${eventId}`);
      }}>
      <CardContent className="flex flex-col gap-3 p-3 md:flex-row">

        <EventImage />

        <div className="
          grid
          w-full
          grid-cols-[1fr_1fr]
          md:grid-cols-[3fr_1fr]
          grid-rows-[auto_auto_auto]
          [grid-template-areas:'sport_loc'_'name_dist'_'desc_desc']
          md:[grid-template-areas:'sport_loc'_'name_loc'_'desc_dist']
          min-w-0
          gap-3
        ">

          <div className="flex flex-wrap items-center gap-2 [grid-area:sport]">

            <Badge variant="secondary">
              {event.sportName}
            </Badge>

            <Badge variant="outline">
              <Users className="mr-1 h-3 w-3" />
              {event.participantCount} / {event.maxParticipants}
            </Badge>

          </div>

          <div className="ml-auto [grid-area:loc] w-full">
            <div className="flex flex-nowrap items-center gap-2 h-full justify-center">
              <MapPin className="mr-1 h-5 w-5" />
              <div className="md:text-lg">
                {event.venueName}
              </div>
            </div>
          </div>

          <div className="text-center text-2xl md:text-5xl md:font-thin [grid-area:dist]">
            {event.distance_km} km
          </div>

          <h2 className="text-lg md:text-2xl font-semibold [grid-area:name]">
            {event.name}
          </h2>

          <div className="rounded-md bg-muted p-3 [grid-area:desc]">
            <p className="text-sm text-muted-foreground line-clamp-2">
              {event.description}
            </p>
          </div>

        </div>
      </CardContent>
    </Card>
    // </Link>
  );
}
