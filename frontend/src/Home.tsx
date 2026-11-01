import './styles/Home.css'

import { Separator } from "@/components/ui/separator";

import {EventCard, Filters} from "@/Events.tsx";


function WelcomeMessage({ username }: { username: string }) {
  return (
    <section className="space-y-2">
      <p className="text-lg text-muted-foreground">
        Welcome {username}
      </p>
      <Separator />
    </section>
  );
}


export default function HomeFeed() {
  return (
    <div className="mx-auto flex flex-col gap-8">

      <WelcomeMessage username="username" />

      <Filters />

      <EventCard />
      <EventCard />
    </div>
  );
}