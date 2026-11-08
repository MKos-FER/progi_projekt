import {useParams} from "react-router-dom";
import {Card, CardContent, CardTitle, CardDescription} from "@/components/ui/card";
import {useEventDetails} from "@/apiStore/useEvents.ts";
import { useState } from "react";
import { Button } from "@/components/ui/button";
import type { EventDetails } from "@/apiStore/useEvents";
import {Badge} from "@/components/ui/badge";
import { Users } from "lucide-react";
import { ChevronsLeft } from "lucide-react";

export default function EventPage() {
  const { eventId } = useParams();
  const id = Number(eventId);

  const { data: event, isLoading, error } = useEventDetails(id);

  console.log("EVENT:", event);
  console.log("LOADING:", isLoading);
  console.log("ERROR:", error);

  if (!eventId || isNaN(eventId)) {
    return <p>Neispravan ID eventa.</p>;
  }

  if (isLoading) {
    return <p>Učitavanje...</p>;
  } else if(error) {
    return <p>Greška pri učitavanju eventa.</p>;
  } else {
    return (
    <>
      event: {eventId}
      <EventDescription event={event} />
      <PeopleInfo event={event} />
      <ChatBox/>
      <TextingBox/>
    </>
    )
  }
  


}

function EventDescription({ event }: { event: EventDetails }) {
 //const { data: event, isLoading, error } = useEventDetails(id);
 const [expanded, setExpanded] = useState(false);

 const maxLength = 150;
 const description = event.description ?? "";
 const isLong = description.length > maxLength;

  const displayedDescription =
    expanded || !isLong
      ? description
      : description.slice(0, maxLength) + "...";

  return (
      
      <Card>
        <CardContent className="flex min-w-0 flex-col gap-3 p-3 md:flex-row" >
            <EventImage/>

            <div className="flex min-w-0 flex-col gap-3" justify>
            <CardTitle className="text-lg md:text-2xl">{ event.name }</CardTitle>

            <CardDescription>
              <p className="break-words whitespace-normal text-sm md:text-base">Opis grupe: {displayedDescription}</p>

              {isLong && (
              <button
                onClick={() => setExpanded(!expanded)}
                className="mt-2 text-blue-500 hover:underline"
              >
                {expanded ? "Prikaži manje" : "Prikaži više"}
              </button>
            )}
            </CardDescription>

            </div>

        </CardContent>
      </Card>
  );
}

function PeopleInfo({ event }: { event: EventDetails }) {
  return (
    <>
    <div>
    <Badge>
      <ChevronsLeft />
    </Badge>

      <Badge variant="outline">
        <Users className="mr-1 h-3 w-3" />
        {event.participants.length} / {event.max_participants}
      </Badge>

    <Button variant="secondary">Sudionici</Button>
    </div>

    </>
  );
}

function ChatBox(){
  return (
    <>
    <div className="flex w-full gap-3">
      <div className="flex flex-1 flex-col">
    
      <div className="min-h-120 w-full rounded-xl bg-gray-100">Chat</div>
    
    </div>
  
  </div>
    </>

  );
}
function TextingBox(){
  return (
    <>

    <Button variant="outline">PRIDRUŽI SE</Button>
    </>

  );
}

function EventImage() {
  return (
    <div className="flex min-h-40 w-full items-center justify-center
                    rounded-md border bg-muted md:w-60 md:h-52 md:shrink-0">
      <span className="text-sm text-muted-foreground">
        slika eventa
      </span>
    </div>
  );
}
