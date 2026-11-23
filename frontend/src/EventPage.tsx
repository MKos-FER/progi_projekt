import { useParams } from "react-router-dom";
import { Card, CardContent, CardTitle, CardDescription } from "@/components/ui/card";
import { useEventDetails, useEventMembers } from "@/apiStore/useEvents.ts";
import { useSkillLevels } from "@/apiStore/useSif.ts";
import { MessageScrollerGroupChat } from "./ChatTest.tsx";
import type { EventDetails } from "@/types/event";
import { useState } from "react";
import { cn, interpolateColor } from "@/lib/utils";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { Users, ChevronLeft, ChevronDown, ChevronUp } from "lucide-react";
import { Skeleton } from "@/components/ui/skeleton"



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

  const { data: event, isLoading, error } = useEventDetails(id);

  if (!eventId || isNaN(Number(eventId))) {
    return <p>Neispravan ID eventa.</p>;
  }

  if (isLoading) {
    return (
      <>
        <div className="mt-7">
          <SkeletonEventInfo />
        </div>
      </>
    );
  } else if (error || !event) {
    return <p>Greška pri učitavanju eventa.</p>;
  } else {
    // TODO: Responzivan dizajn (napravit da je vanjski div non-scrollable)
    return (
      <div className="mt-3 flex flex-col min-h-0 h-full">
        <EventDescription event={event} />
        <PeopleInfo event={event} />
        <ChatBox event={event} />
      </div>
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

  // TODO: Napraviti da se Card ne može kompresirat
  return (
    <Card className="min-h-50">
      <CardContent className="flex min-w-0 flex-col gap-3 p-3 md:flex-row" >
        <EventImage />

        <div className="flex min-w-0 flex-col gap-3" >
          <CardTitle className="text-lg md:text-2xl">{event.name}</CardTitle>

          <CardDescription>
            <p className="break-words whitespace-normal text-sm md:text-base">{displayedDescription}</p>

            {isLong && (
              <button
                onClick={() => setExpanded(!expanded)} className="mt-2 text-primary hover:underline">

                {expanded ? <ChevronUp className="" /> : <ChevronDown />}

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
          {event.participantCount} / {event.maxParticipants}
        </Badge>
        <div>
          <ShowMembersButton event={event} />
        </div>
      </div>

    </>
  );
}

function ShowMembersButton({ event }: { event: EventDetails }) {
  const [openMemberInfo, setOpenMemberInfo] = useState(false)
  const [clickedMemberIdx, setClickedMemberIdx] = useState(-1)
  const isMobile = useIsMobile()

  const { data: memberList, isLoading, error } = useEventMembers(event.id)
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
            <FieldLabel key={member.userId} className="mb-2" onClick={() => handleClickUser(memberListIdx)}>
              <Field orientation="horizontal">
                <Avatar className="self-center">
                  <AvatarImage
                    src={member.profilePhotoUrl ?? ""}
                    alt={`@${member.username}`}
                  />
                  <AvatarFallback>{member.name[0]}</AvatarFallback>
                </Avatar>
                <FieldContent>
                  <FieldTitle className="flex items-center gap-2">
                    {member.name}
                    {member.userId === event.organizerId ? (
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

                  <p className="mb-2">Sudionikovi sportovi:</p>
                  {clickedMember.sports?.map((sport) =>
                    <SkillLevelBar key={sport.sportId} level={sport.skillLevelId} name={sport.sportName} />
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
    </Drawer >
  )
}

export function SkillLevelBar({ name: sportName, level }: {name: string, level: number }) {
  // TODO: Promijeniti u primary i secondary / boje teme kada budu dodane
  const startColor = "oklch(0.8468 0.1532 133.28)";
  const endColor = "oklch(0.721 0.1359 150.47)";

  const { data: skillLevels, isLoading, error } = useSkillLevels();

  if (isLoading || error) {
    return;
  }

  const levelExists = skillLevels.find(
    (skill) => skill.id === level
  );

  if (!levelExists) {
    return
  }

  return (
    <div className="w-full space-y-2 mb-4 rounded-2xl shadow-sm p-2">
      <div className="flex items-center justify-center mb-2">
        <span className="text-sm font-medium">
          {sportName}
        </span>
      </div>

      {/* Prazni pravokutnici koji predstavljaju bar */}
      <div className="flex gap-1.5 my-0">
        {skillLevels.map((skill, index) => (
          <div
            key={skill.id}
            className={cn(
              "h-2 flex-1 rounded-full transition-colors",
              skill.id > level && "bg-muted"
            )}
            style={
              skill.id <= level
                ? { backgroundColor: interpolateColor(startColor, endColor, index/(skillLevels.length - 1)) }
                : undefined
            }
          />
        ))}
      </div>

      {/* Imena skilova spod bara */}
      <div className="flex gap-1.5">
        {skillLevels.map((skill) => (
          <div key={skill.id} className="flex-1 text-center">
            <span
              className={cn(
                "text-[10px] leading-tight",
                skill.id === level
                  ? "font-semibold text-foreground"
                  : "text-muted-foreground opacity-30"
              )}
            >
              {skill.name}
            </span>
          </div>
        ))}
      </div>
    </div>
  );
}

function ChatBox({ event }: { event: EventDetails }) {
  // TODO: Napraviti da se ChatBox ne može kompresirati
  return (
    <div className="flex grow min-h-50 max-h-155 w-full overflow-y-auto rounded-xl bg-gray-100">
      <MessageScrollerGroupChat event={event} />
    </div>
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

function SkeletonEventInfo() {
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
                <Skeleton className="w-22" />
              </Badge>

            </div>

            <Skeleton className="rounded-md bg-muted p-3 text-sm text-muted-foreground [grid-area:desc]" />

          </div>
        </CardContent>
      </Card>
    </>
  );
}
