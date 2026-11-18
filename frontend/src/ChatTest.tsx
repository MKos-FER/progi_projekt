import * as React from "react"
import { CircleX, RotateCwIcon } from "lucide-react"

import { Bubble, BubbleContent } from "@/components/ui/bubble"
import { Button } from "@/components/ui/button"
import {
  Card,
  CardContent,
  CardFooter,
} from "@/components/ui/card"
import { Marker, MarkerContent } from "@/components/ui/marker"
import {
  Message,
  MessageAvatar,
  MessageContent,
  MessageGroup,
} from "@/components/ui/message"
import {
  MessageScroller,
  MessageScrollerButton,
  MessageScrollerContent,
  MessageScrollerItem,
  MessageScrollerProvider,
  MessageScrollerViewport,
} from "@/components/ui/message-scroller"
import {
  Tooltip,
  TooltipContent,
  TooltipTrigger,
} from "@/components/ui/tooltip"
import { Avatar, AvatarFallback, AvatarImage } from "@/components/ui/avatar"
import { getDefaultAvatar } from "./lib/utils"
import { useIsAuth } from "./apiStore/useUsers"
import { Textarea } from "@/components/ui/textarea"
import { Link } from "react-router-dom";
import { useJoinEvent, useMyEvents } from "@/apiStore/useEvents"
import type { EventDetails } from "@/types/event"
import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert"
import { Separator } from "@base-ui/react"

const currentUser = "Mateo Horvat"

const initialItems = [
  {
    id: "1",
    type: "message",
    sender: "Albert",
    role: "participant",
    text: "Nisi puno napisala u opisu, ima li nekih posebnih pravila?",
  },
  {
    id: "2",
    type: "messageGroup",
    sender: "Marija (Organizator)",
    role: "organiser",
    text: ["A pa dosta standardno, nema guranja, loptu donosiš sam",
      "E al mislila sam možda kasnije neki roštilj nabacit pa recite ako ste za da znam koliko nabaviti"],
  },
  {
    id: "3",
    type: "message",
    sender: "Albert",
    role: "participant",
    text: "Yoooo, roštilj je super ideja",
    scrollAnchor: true,
  },
] satisfies GroupChatItem[]

const joinMarker = {
  id: "marker-1",
  type: "event",
  text: currentUser + " has joined the event",
  scrollAnchor: false,
} satisfies GroupChatItem

const newMessage = [
  {
    id: "4",
    type: "messageGroup",
    sender: currentUser,
    role: "participant",
    text: ["Ja sam samo za roštilj ušao, mogu nabavit ako što još treba", "Samo javi kad da dođem"],
    scrollAnchor: false,
  },
  {
    id: "5",
    type: "message",
    sender: "Marija (Organizator)",
    role: "organiser",
    text: "Ne treba ništa, dođi u 5",
    scrollAnchor: false,
  },
] satisfies GroupChatItem[]

type GroupChatItem =
  | {
    id: string
    type: "event"
    text: string
    scrollAnchor?: boolean
  }
  | {
    id: string
    type: "message"
    sender: string
    role: "organiser" | "participant"
    text: string
    scrollAnchor?: boolean
  }
  | {
    id: string
    type: "messageGroup"
    sender: string
    role: "organiser" | "participant"
    text: string[]
    scrollAnchor?: boolean
  }

// function AuthDebug() {
//   const { data: isAuth } = useIsAuth();
//
//   const queryClient = useQueryClient();
//
//   React.useEffect(() => {
//     return queryClient.getQueryCache().subscribe((event) => {
//       if (event.query.queryKey[0] === "isAuth") {
//         console.log(
//           "CACHE EVENT:",
//           event.type,
//           event.query.state.data
//         );
//       }
//     });
//   }, [queryClient]);
//
//   console.log("AuthDebug render:", isAuth);
//
//   return <div>{String(isAuth)}</div>;
// }

export function MessageScrollerGroupChat({ event }: { event: EventDetails }) {
  const { data: isAuth } = useIsAuth()

  const [demoKey, setDemoKey] = React.useState(0)
  const [rockyTurn, setRockyTurn] = React.useState<
    "idle" | "marker" | "message"
  >("idle")
  const items =
    rockyTurn === "message"
      ? [...initialItems, joinMarker, ...newMessage]
      : rockyTurn === "marker"
        ? [...initialItems, joinMarker]
        : initialItems
  const buttonLabel =
    rockyTurn === "idle" ? "Već si član eventa" : "Pošalji poruku"
  const isComplete = rockyTurn === "message"

  const { mutate: joinEvent, isPending: joinIsPending, error: joinError } = useJoinEvent()
  const { data: myEvents, isLoading: myeIsLoading, error: myeError } = useMyEvents()

  if (myeIsLoading || !myEvents) {
    // TODO: join button skeleton
    return;
  }

  if (myeError) {
    return (
      <div>
        ERROR
      </div>
    );
  }

  console.log(joinIsPending)

  const isInEvent = myEvents.findIndex(e => e.id === event.id) !== -1

  return (
    <MessageScrollerProvider>
      <Card className="mx-auto w-full gap-0 ">
        <CardContent className="min-h-0 flex-1 p-0">
          <MessageScrollerProvider>
            <MessageScroller key={demoKey}>
              <MessageScrollerViewport>
                <MessageScrollerContent className="p-(--card-spacing)">
                  {items.map((item) => {
                    if (item.type === "message") {
                      return (
                        <GroupChatMessage key={item.id} item={item} />
                      );
                    } else if (item.type === "messageGroup") {
                      return (
                        <GroupChatMultiMessage key={item.id} item={item} />
                      );
                    } else {
                      return (
                        <GroupChatMarker
                          key={item.id}
                          item={item}
                          scrollAnchor={item.scrollAnchor}
                        />
                      )
                    }
                  })
                  }
                </MessageScrollerContent>
              </MessageScrollerViewport>
              <MessageScrollerButton />
            </MessageScroller>
          </MessageScrollerProvider>
        </CardContent>
        {isAuth && <CardFooter className="flex flex-row items-center gap-2 border-t">
          <Tooltip>
            <TooltipTrigger render={<Button type="button" variant="outline" size="icon" aria-label="Reset conversation" disabled={rockyTurn === "idle"} onClick={() => {
              setRockyTurn("idle")
              setDemoKey((key) => key + 1)
            }}><RotateCwIcon /></Button>} />
            <TooltipContent>
              <p>Reset Conversation</p>
            </TooltipContent>
          </Tooltip>
          <div className="flex-col w-full flex gap-2">
            {isInEvent && <Button
              type="button"
              disabled={isComplete}
              onClick={() =>
                setRockyTurn((turn) => (turn === "idle" ? "marker" : "message"))
              }
              className="grow"
              variant="outline"
            >
              {buttonLabel}
            </Button>}
            {!isInEvent && <Button
              type="button"
              disabled={joinIsPending}
              onClick={() => {
                joinEvent(event.id);
              }}
              className="grow"
              variant="outline"
            >
              Pridruži se
            </Button>}
            {joinError &&
              <Alert key="alert" className="text-destructive *:[svg]:text-current bg-destructive-foreground">
                <CircleX />
                <AlertTitle>Dogodila se greška prilikom pokušaja ulaska u događaj</AlertTitle>
                <AlertDescription>
                  {joinError.message}
                </AlertDescription>
              </Alert>
            }
          </div>
        </CardFooter>}
        <TextingBox />
      </Card>
    </MessageScrollerProvider>
  )
}

function GroupChatMessage({
  item,
}: {
  item: Extract<GroupChatItem, { type: "message" }>
}) {
  const isCurrentUser = item.sender === currentUser
  const variant = isCurrentUser
    ? "default" : "muted"

  const bubbleClass = isCurrentUser ? "my-chat-bubble" : "other-chat-bubble";

  return (
    <MessageScrollerItem messageId={item.id} scrollAnchor={item.scrollAnchor}>
      <Message align={isCurrentUser ? "end" : "start"}>
        <MessageAvatar className="self-start">
          {!isCurrentUser &&
            <Avatar>
              <AvatarImage src={item.role === "organiser" ? getDefaultAvatar("28") ?? "" : getDefaultAvatar("22") ?? ""} alt="username" />
              <AvatarFallback>R</AvatarFallback>
            </Avatar>
          }
        </MessageAvatar>
        <MessageContent>
          <Bubble variant={variant} className={bubbleClass}>
            <BubbleContent>
              {!isCurrentUser && <div className="text-blue-700 mb-1 mt-[-0.2rem]">
                {item.sender}
              </div>}
              {item.text}
            </BubbleContent>
          </Bubble>
        </MessageContent>
      </Message>
    </MessageScrollerItem>
  )
}

function GroupChatMultiMessage({
  item,
}: {
  item: Extract<GroupChatItem, { type: "messageGroup" }>
}) {
  const isCurrentUser = item.sender === currentUser
  const variant = isCurrentUser
    ? "default" : "muted"

  const bubbleClass = isCurrentUser ? "my-chat-bubble" : "other-chat-bubble";

  return (
    <MessageScrollerItem messageId={item.id} scrollAnchor={item.scrollAnchor}>
      <MessageGroup>
        {
          item.text.map((message, messageIdx) => {
            if (messageIdx === 0) {
              return (
                <Message key={item.id + " " + messageIdx} align={isCurrentUser ? "end" : "start"}>
                  <MessageAvatar className="self-start">
                    {!isCurrentUser &&
                      <Avatar>
                        <AvatarImage src={getDefaultAvatar("28") ?? ""} alt="username" />
                        <AvatarFallback>R</AvatarFallback>
                      </Avatar>
                    }
                  </MessageAvatar>
                  <MessageContent>
                    <Bubble variant={variant} className={bubbleClass}>
                      <BubbleContent>
                        {!isCurrentUser && <div className="text-blue-700 mb-1 mt-[-0.2rem]">
                          {item.sender}
                        </div>}
                        {message}
                      </BubbleContent>
                    </Bubble>
                  </MessageContent>
                </Message>
              );
            } else {
              return (
                <Message key={item.id + " " + messageIdx} align={isCurrentUser ? "end" : "start"}>
                  <MessageAvatar>
                  </MessageAvatar>
                  <MessageContent>
                    <Bubble variant={variant} >
                      <BubbleContent>{message}</BubbleContent>
                    </Bubble>
                  </MessageContent>
                </Message>
              );
            }
          })
        }
      </MessageGroup>
    </MessageScrollerItem>
  )
}

function GroupChatMarker({
  item,
  scrollAnchor = false,
}: {
  item: Extract<GroupChatItem, { type: "event" }>
  scrollAnchor?: boolean
}) {
  return (
    <MessageScrollerItem scrollAnchor={scrollAnchor}>
      <Marker variant="separator">
        <MarkerContent>{item.text}</MarkerContent>
      </Marker>
    </MessageScrollerItem>
  )
}


export function TextingBox() {
  const { data: isAuth } = useIsAuth();

  if (!isAuth) {
    return (
      <>
        <Link to="/login">
          <Button className="mt-3 w-full" variant="outline">Kako bi se pridružio grupi moraš se ulogirati</Button>
        </Link>
      </>

    );
  } else {
    return;

    // TODO: Iskoristiti za pisanje poruke umjesto simulacije
    return (
      <>
        <div className="relative mt-4">
          <Textarea placeholder="Napiši poruku..." />

          <Button className="absolute bottom-3 right-3">
            Send
          </Button>
        </div>
      </>

    );
  }
}
