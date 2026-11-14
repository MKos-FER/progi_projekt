import { useParams } from "react-router-dom";
import { Card, CardContent, CardTitle, CardDescription } from "@/components/ui/card";
import { useCancelEvent, useEventDetails, useEventMembers } from "@/apiStore/useEvents.ts";
import type { EventDetails } from "@/types/event";
import { useState } from "react";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { Users, ChevronLeft, ChevronDown, ChevronUp } from "lucide-react";
import { Skeleton } from "@/components/ui/skeleton"
import { Textarea } from "@/components/ui/textarea"
import { Link } from "react-router-dom";



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
import { Progress } from "@/components/ui/progress"

export default function EventPage() {
  const { eventId } = useParams();
  const id = Number(eventId);
  const [onboarded, setOnboarded] = useState(false)

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
    return(
      <>
      <div className="mt-7">
      <SkeletonEventInfo/>
      </div>
      </>
    );
  } else if (error || !event) {
    return <p>Greška pri učitavanju eventa.</p>;
  } else {
    // TODO: DELETE THE DELETE BUTTON
    return (
      <>
        <Button onClick={() => deleteEvent(id)} >DELETE EVENT</Button>
        <EventDescription event={event} />
        <PeopleInfo event={event} />
        <ChatBox />
        <TextingBox onboarded={ onboarded }/>
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
    //TODO: promijeniti boje ikonica ChevronUp i ChevronDown u skladu s paletom stranice s text-boja-value

    <Card>
      <CardContent className="flex min-w-0 flex-col gap-3 p-3 md:flex-row" >
        <EventImage />

        <div className="flex min-w-0 flex-col gap-3" >
          <CardTitle className="text-lg md:text-2xl">{event.name}</CardTitle>

          <CardDescription>
            <p className="break-words whitespace-normal text-sm md:text-base">Opis grupe: {displayedDescription}</p>

            {isLong && (
              <button
                onClick={() => setExpanded(!expanded)} className="mt-2 text-blue-500 hover:underline">

                  {expanded ? <ChevronUp className=""/> : <ChevronDown />}
                  
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
      <div className=" mt-4 mb-4 flex  w-full items-center justify-between ">
        
        <Badge className="" variant="outline">
          <Users className="h-3 w-3" />
          {event.participants.length} / {event.max_participants}
        </Badge>
        <div>
        <ShowMembersButton event={event}/>
        </div>
      </div>

    </>
  );
}

function ShowMembersButton({event}: {event: EventDetails}) {
  const [openMemberInfo, setOpenMemberInfo] = useState(false)
  const [clickedMemberIdx, setClickedMemberIdx] = useState(-1)
  const isMobile = useIsMobile()

  const {data: memberList, isLoading, error} = useEventMembers(event.id)
  const swipeDirection = isMobile ? "down" : "right";

  const clickedMember = memberList?.[clickedMemberIdx] ?? null;


  if (error) {
    return
  }

  if (isLoading) {
    return
  }

  if (!memberList) {
    return;
  }

  function handleClickUser(memberListIdx: number) {
    setClickedMemberIdx(memberListIdx);
    setOpenMemberInfo(true);
  }

  return (
    <Drawer
      showSwipeHandle={isMobile}
      swipeDirection={swipeDirection}
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
          {memberList.map((member, memberListIdx) => (
            <FieldLabel key={member.id} className="mb-2" onClick={() => handleClickUser(memberListIdx)}>
              <Field orientation="horizontal">
                <Avatar className="self-center">
                  <AvatarImage
                    src={member.profile_photo_url??""}
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

          {/* Prikaz info jednog sudionika */}
          <Drawer
            open={openMemberInfo}
            onOpenChange={setOpenMemberInfo}
            showSwipeHandle={isMobile}
            swipeDirection={swipeDirection}
          >
            <DrawerContent>
              <DrawerHeader>
                <DrawerTitle>Informacije o sudioniku</DrawerTitle>
              </DrawerHeader>
              {clickedMember && (
              <div className="flex-1 scroll-fade overflow-y-auto p-4">

                <div className="flex min-h-32 w-full items-center justify-center
                  rounded-md border bg-muted">
                  <span className="text-sm text-muted-foreground">
                    slika sudionika
                  </span>
                </div>

                <div className="grid grid-cols-2 mb-6 pr-1">
                  <div className="text-2xl">
                    {clickedMember.name}
                  </div>
                  <div className="text-lg text-right self-center text-muted-foreground">
                    {clickedMember.username}
                  </div>
                </div>

                Skills:
                {clickedMember.user_sports?.map((sport) =>
                  <div key={sport.id} className="flex justify-between mb-2">
                    {sport.sportName}
                    <Progress value={sport.skill_level_id/2*100} className="w-[50%] inline-block self-center" />
                  </div>
                )}
              </div>
              )}
              <DrawerFooter>
                <DrawerClose render={<Button variant="outline"><ChevronLeft /> Natrag na popis sudionika</Button>} />
              </DrawerFooter>
            </DrawerContent>
          </Drawer>


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
function TextingBox({ onboarded }: { onboarded: boolean }) {
  
  if(!onboarded){
    return (
    <>
    <Link to="/login">
      <Button className="mt-3" variant="outline">PRIDRUŽI SE</Button>
      </Link>
    </>

    );
  } else {
    return (
    <>
      <div className="relative mt-4">
  <Textarea placeholder="Napiši poruku..."/>

  <Button className="absolute bottom-3 right-3">
    Send
  </Button>
</div>
    </>

  );
  }
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

function SkeletonEventInfo () {
  return (
    <>
    <Card>
      <CardContent className="flex flex-col gap-3 p-3 md:flex-row">

        <Skeleton className="flex min-h-40 w-full items-center justify-center
                    rounded-md border bg-muted md:h-52 md:w-60 md:shrink-0"/>

        <div className="
          grid
          w-full
          grid-cols-[1fr_1fr]
          grid-rows-[auto_auto_auto]
          [grid-template-areas:'sport_loc'_'name_dist'_'desc_desc']
          md:[grid-template-areas:'name'_'desc']
          min-w-0
          gap-3
        ">

          <div className="flex flex-wrap items-center [grid-area:name]">

            <Badge variant="secondary">
              <Skeleton className="w-22"/>
            </Badge>

          </div>

          <Skeleton className="rounded-md bg-muted p-3 text-sm text-muted-foreground [grid-area:desc]"/>

        </div>
      </CardContent>
    </Card>
    </>
  );
}
