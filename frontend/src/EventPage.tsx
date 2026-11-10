import { useParams } from "react-router-dom";
import { Card, CardContent, CardTitle, CardDescription } from "@/components/ui/card";
import { useCancelEvent, useEventDetails, useEventMembers } from "@/apiStore/useEvents.ts";
import type { EventDetails } from "@/types/event";
import type { PublicUserInfo } from "@/types/user";
import { useState } from "react";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { Users, ChevronLeft } from "lucide-react";
import { ChevronsLeft } from "lucide-react";

import { useIsMobile } from "@/hooks/use-mobile"
import {
  Drawer,
  DrawerClose,
  DrawerContent,
  DrawerDescription,
  DrawerFooter,
  DrawerHeader,
  DrawerTitle,
  DrawerTrigger,
} from "@/components/ui/drawer"
import {
  Field,
  FieldContent,
  FieldDescription,
  FieldLabel,
  FieldTitle,
} from "@/components/ui/field"
import {
  Avatar,
  AvatarFallback,
  AvatarImage,
} from "@/components/ui/avatar"

export default function EventPage() {
  const { eventId } = useParams();
  const id = Number(eventId);

  const { data: event, isLoading, error } = useEventDetails(id);

  const {mutate: deleteEvent, error: cancelError} = useCancelEvent()
  if (cancelError) {
    return (
      <p>ERROR: {cancelError.message}</p>
    );
  }

  console.log("EVENT:", event);
  console.log("LOADING:", isLoading);
  console.log("ERROR:", error);

  if (!eventId || isNaN(Number(eventId))) {
    return <p>Neispravan ID eventa.</p>;
  }

  if (isLoading) {
    return <p>Učitavanje...</p>;
  } else if (error) {
    return <p>Greška pri učitavanju eventa.</p>;
  } else {
    // TODO: DELETE THE DELETE BUTTON
    return (
      <>
        event: {eventId}
        <Button onClick={() => deleteEvent(id)} >DELETE EVENT</Button>
        <EventDescription event={event} />
        <PeopleInfo event={event} />
        <ChatBox />
        <TextingBox />
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
        <EventImage />

        <div className="flex min-w-0 flex-col gap-3" >
          <CardTitle className="text-lg md:text-2xl">{event.name}</CardTitle>

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

        <ShowMembersButton event={event}/>
      </div>

    </>
  );
}

function ShowMembersButton({event}: {event: EventDetails}) {
  const [open, setOpen] = useState(false)
  // const [clickedMemberId, setClickedMemberId] = useState(0)
  const isMobile = useIsMobile()

  const {data: memberList, isLoading, error} = useEventMembers(event.id)

  if (error) {
    return
  }

  if (isLoading) {
    return
  }

  if (!memberList) {
    return;
  }

  function handleConfirm() {
    setOpen(false)
  }

  return (
    <Drawer
      open={open}
      onOpenChange={setOpen}
      showSwipeHandle={isMobile}
      swipeDirection={isMobile ? "down" : "right"}
    >
      <DrawerTrigger render={<Button variant="secondary">Sudionici</Button>} />
      <DrawerContent>
        <DrawerHeader>
          <DrawerTitle>Sudionici</DrawerTitle>
          <DrawerDescription>
            Popis svih korisnika koji su potvrdili dolazak na događaj.
          </DrawerDescription>
        </DrawerHeader>
        <div className="flex-1 scroll-fade overflow-y-auto p-4">
          {memberList.map((member) => (
            <FieldLabel key={member.id} className="mb-2">
              <Field orientation="horizontal">
                <Avatar className="self-center">
                  <AvatarImage
                    src={member.profile_photo_url}
                    alt={`@${member.username}`}
                  />
                  <AvatarFallback>{member.name[0]}</AvatarFallback>
                </Avatar>
                <FieldContent>
                  <FieldTitle className="flex items-center gap-2">
                    {member.name}
                    {member.id === event.created_by_id ? (
                      <Badge variant="secondary" className="bg-blue-50 text-blue-700">Organizator</Badge> // TODO: zamijeniti boje s nekima iz teme
                    ) : null}
                  </FieldTitle>
                  <FieldDescription>{member.username}</FieldDescription>
                </FieldContent>
              </Field>
            </FieldLabel>
          ))}
        </div>
        <DrawerFooter>
          <DrawerClose render={<Button variant="outline"><ChevronLeft /> Natrag na stranicu događaja</Button>} />
        </DrawerFooter>
      </DrawerContent>
    </Drawer>
  )
}

function ChatBox() {
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
function TextingBox() {
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
