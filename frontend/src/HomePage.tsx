import './styles/Home.css'

import {Separator} from "@/components/ui/separator";

import {EventCard, Filters, SkeletonEventCard} from "@/MyEventsPage.tsx";
import {useEvents} from "@/apiStore/useEvents.ts";
import type {JSX} from "react";
import {Alert, AlertTitle, AlertDescription} from "@/components/ui/alert.tsx";
import {CircleX} from 'lucide-react'


function WelcomeMessage({username}: { username: string }) {
  return (
    <section className="space-y-2">
      <p className="text-lg text-muted-foreground">
        Welcome {username}
      </p>
      <Separator/>
    </section>
  );
}


export default function HomeFeed() {
  const {data: events, isLoading, error} = useEvents();
  const {isFetching, status, fetchStatus} = useEvents();

  console.log("EVENTS:", {
    isLoading,
    isFetching,
    status,
    fetchStatus,
    error,
  });

  let displayEvents: JSX.Element[] = [];

  if (isLoading) {
    displayEvents.push(<SkeletonEventCard key={0}/>);
    displayEvents.push(<SkeletonEventCard key={1}/>);
    displayEvents.push(<SkeletonEventCard key={2}/>);
    displayEvents.push(<SkeletonEventCard key={3}/>);
    displayEvents.push(<SkeletonEventCard key={4}/>);
    displayEvents.push(<SkeletonEventCard key={5}/>);
  } else if (!error) {
    displayEvents = events!.map(event => (
      <EventCard key={event.id} eventId={event.id}/>
    ))
  } else {
    displayEvents = [
      <Alert key="alert" className="text-destructive *:[svg]:text-current bg-destructive-foreground">
        <CircleX />
        <AlertTitle>Dogodila se greška prilikom dohvata novih događaja</AlertTitle>
        <AlertDescription>
          Prilikom dohvata novih događaja u vašoj okolini došlo je do pogreške na serverskoj strani,
          ostanite strpljivi i pokušajte ponovno kasnije.
          <Separator className={"my-3"}/>
          Error info: {error.message}
        </AlertDescription>
      </Alert>
    ]
  }

  return (
    <div className="mx-auto flex flex-col gap-8">

      <WelcomeMessage username="username"/>

      <Filters/>

      {displayEvents}
    </div>
  );
}