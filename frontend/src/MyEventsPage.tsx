import {useEvents, useMyEvents} from "@/apiStore/useEvents.ts";
import type {EventCardInfo} from "@/types/event.ts"
import {Skeleton} from "@/components/ui/skeleton"
import {useState, type JSX} from "react";

import {Button} from "@/components/ui/button";
import {Card, CardContent} from "@/components/ui/card";
import {Badge} from "@/components/ui/badge";
import {
  MapPin,
  Users,
  Filter, CircleX,
} from "lucide-react";
import {Link, useNavigate} from "react-router-dom";
import {Alert, AlertDescription, AlertTitle} from "@/components/ui/alert.tsx";
import {Separator} from "@/components/ui/separator.tsx";
import {Item, ItemActions, ItemContent} from "@/components/ui/item.tsx";

export default function MyEvents() {
  const {data: events, isLoading, error} = useMyEvents();
  const navigate = useNavigate();

  let displayEvents: JSX.Element[] = [];

  if (error) {
    return (
      <div className="my-4 mx-auto flex flex-col gap-8">
        <Alert className="text-destructive *:[svg]:text-current bg-destructive-foreground">
          <CircleX/>
          <AlertTitle>Dogodila se greška prilikom dohvata novih događaja</AlertTitle>
          <AlertDescription>
            Prilikom dohvata novih događaja u vašoj okolini došlo je do pogreške na serverskoj strani,
            ostanite strpljivi i pokušajte ponovno kasnije.
            <Separator className={"my-3"}/>
            Error info: {error.message}
          </AlertDescription>
        </Alert>
      </div>
    );
  }

  if (events) {
    displayEvents = events.map(event => (
      <EventCard key={event.id} eventId={event.id}/>
    ))
  }

  if (isLoading) {
    displayEvents.push(<SkeletonEventCard key={0}/>);
    displayEvents.push(<SkeletonEventCard key={1}/>);
    displayEvents.push(<SkeletonEventCard key={2}/>);
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
    <div className="my-4 mx-auto flex flex-col gap-8">
      {displayEvents}
    </div>
  );
}


export function Filters() {
  const [open, setOpen] = useState(false);

  return (
    <section className="space-y-3">

      <Button
        variant="outline"
        onClick={() => setOpen(!open)}
        aria-expanded={open}
      >
        <Filter className="mr-2 h-4 w-4"/>
        FILTERI
      </Button>

      <Card className={`overflow-hidden animate-in fade-in slide-in-from-top-2 duration-120
                        ease-in-out ${open ? "max-h-45 opacity-100" : "max-h-0 opacity-0 m-0 p-0"}`}>
        <CardContent className="p-4">
          <p className="text-sm text-muted-foreground">
            Ovdje se prikazuju dostupni filteri
          </p>
        </CardContent>
      </Card>

      <Card>
        <CardContent className="flex min-h-10 flex-wrap items-center gap-2 p-3">
          <span className="text-sm text-muted-foreground">
            Ovdje se prikazuju odabrani filteri
          </span>
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
              <Skeleton className="w-22"/>
            </Badge>

            <Badge variant="outline">
              <Skeleton className="w-12"/>
            </Badge>

          </div>

          <div className="ml-auto [grid-area:loc] w-full">
            <div className="flex flex-nowrap items-center gap-2 h-full justify-center">
              <Skeleton className="w-30 h-5"/>
            </div>
          </div>

          <Skeleton className="text-center text-2xl md:text-5xl md:font-thin [grid-area:dist]">
          </Skeleton>

          <Skeleton className="text-lg md:text-2xl font-semibold [grid-area:name]"/>

          <Skeleton className="rounded-md bg-muted p-3 text-sm text-muted-foreground [grid-area:desc]"/>

        </div>
      </CardContent>
    </Card>
  )
}

export function EventCard({eventId}: { eventId: number }) {
  const {data: events, isLoading, error} = useEvents();
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
      <SkeletonEventCard/>
    )
  }

  const event: EventCardInfo = events!.find(e => e.id == eventId)!
  return (
    // <Link to={`/events/${eventId}`} className="select-auto">
    <Card className="transition-all duration-200 hover:scale-101 active:scale-99"
          onClick={() => { // Ovako je cijeli Card i dalje clickable a text se i dalje može selectat
            if (window.getSelection()?.toString()) return;
            void navigate(`/events/${eventId}`);
          }}>
      <CardContent className="flex flex-col gap-3 p-3 md:flex-row">

        <EventImage/>

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
              <Users className="mr-1 h-3 w-3"/>
              {event.participants.length} / {event.max_participants}
            </Badge>

          </div>

          <div className="ml-auto [grid-area:loc] w-full">
            <div className="flex flex-nowrap items-center gap-2 h-full justify-center">
              <MapPin className="mr-1 h-5 w-5"/>
              <div className="md:text-lg">
                {event.locationName}
              </div>
            </div>
          </div>

          <div className="text-center text-2xl md:text-5xl md:font-thin [grid-area:dist]">
            {event.distance_km} km
          </div>

          <h2 className="text-lg md:text-2xl font-semibold [grid-area:name]">
            {event.name}
          </h2>

          <p className="rounded-md bg-muted p-3 text-sm text-muted-foreground [grid-area:desc]">
            {event.description}
          </p>

        </div>
      </CardContent>
    </Card>
    // </Link>
  );
}