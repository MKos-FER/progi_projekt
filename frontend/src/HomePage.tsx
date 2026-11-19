import './styles/Home.css'

import { Separator } from "@/components/ui/separator";

import { EventCard, Filters, SkeletonEventCard } from "@/MyEventsPage.tsx";
import { useEvents } from "@/apiStore/useEvents.ts";
import { type EventFilters } from "@/types/event";
import { useState, type JSX } from "react";
import { Alert, AlertTitle, AlertDescription } from "@/components/ui/alert.tsx";
import { CircleX, Ghost, InfoIcon } from 'lucide-react'
import { useIsAuth, useUserProfile } from '@/apiStore/useUsers';
import { Button } from '@/components/ui/button';
import { Empty, EmptyHeader, EmptyMedia, EmptyTitle, EmptyDescription, EmptyContent } from '@/components/ui/empty';


function WelcomeMessage() {
  const { data: isAuth } = useIsAuth()
  const { data: user, isLoading, error } = useUserProfile()

  if (!isAuth) {
    return (
      <section className="space-y-2">
        <p className="text-lg mt-1 text-muted-foreground">
          Dobrodošli na Let's Do Sports!
        </p>
        <Alert>
          <InfoIcon />
          <AlertTitle>Niste prijavljeni u aplikaciju</AlertTitle>
          <AlertDescription>
            Prijavom otključate sposobnost najave dolaska na događaj, slanja poruka i monogo drugih pogodnosti.
          </AlertDescription>
        </Alert>
        <Separator />
      </section>
    );
  }

  if (error) return (
    <></>
  );
  if (!user || isLoading) return (
    <section className="space-y-2">
      <p className="text-lg text-muted-foreground">
        Dobrodošli na Let's Do Sports!
      </p>
      <Separator />
    </section>
  );

  return (
    <section className="space-y-2">
      <p className="text-lg text-muted-foreground">
        Dobrodošao {user.name}
      </p>
      <Separator />
    </section>
  );
}

export default function HomeFeed() {
  const [filters, setFilters] = useState<EventFilters | undefined>()

  console.log(filters)
  const { data: events, isLoading, error } = useEvents(filters);

  let displayEvents: JSX.Element[] = [];

  if (isLoading) {
    displayEvents.push(<SkeletonEventCard key={0} />);
    displayEvents.push(<SkeletonEventCard key={1} />);
    displayEvents.push(<SkeletonEventCard key={2} />);
    displayEvents.push(<SkeletonEventCard key={3} />);
    displayEvents.push(<SkeletonEventCard key={4} />);
    displayEvents.push(<SkeletonEventCard key={5} />);
  } else if (!error) {
    displayEvents = events!.map(event => (
      <EventCard key={event.id} eventId={event.id} />
    ))

    if (events!.length === 0) {
      displayEvents = [
        <Empty key="empty-events">
          <EmptyHeader>
            <EmptyMedia variant="icon">
              <Ghost className="size-6" />
            </EmptyMedia>
            <EmptyTitle>Trenutno nema događaja</EmptyTitle>
            {!!filters && <EmptyDescription>
              Trenutno nema aktivnih događaja koji zadovoljavaju tvoje filtere.
            </EmptyDescription>}
            {!filters && <EmptyDescription>
              Nitko se nije sjetio napraviti događaj. To samo znači da možeš biti prvi!
            </EmptyDescription>}
          </EmptyHeader>
          <EmptyContent className="flex-row justify-center gap-2">
            <Button>Kreiraj Događaj</Button>
            {!!filters && <Button variant="outline" onClick={() => setFilters(null)}>Očisti filtere</Button>}
          </EmptyContent>
        </Empty>
      ]
    }
  } else {
    displayEvents = [
      <Alert key="alert" className="text-destructive *:[svg]:text-current bg-destructive-foreground">
        <CircleX />
        <AlertTitle>Dogodila se greška prilikom dohvata novih događaja</AlertTitle>
        <AlertDescription>
          Prilikom dohvata novih događaja u vašoj okolini došlo je do pogreške na serverskoj strani,
          ostanite strpljivi i pokušajte ponovno kasnije.
          <Separator className={"my-3"} />
          Error info: {error.message}
        </AlertDescription>
      </Alert>
    ]
  }

  return (
    <div className="mx-auto flex flex-col gap-6">

      <WelcomeMessage />

      <Filters filters={filters} setFilters={setFilters} />

      {displayEvents}
    </div>
  );
}
