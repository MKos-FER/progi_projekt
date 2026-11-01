export default function MyEvents() {
  return (
    <>
      <div>Moji događaji</div>
    </>
  )
}

import { useState } from "react";

import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import {
  MapPin,
  Users,
  Filter,
} from "lucide-react";

export function Filters() {
  const [open, setOpen] = useState(false);

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

      {open && (
        <Card>
          <CardContent className="p-4">
            <p className="text-sm text-muted-foreground">
              Ovdje se prikazuju dostupni filteri
            </p>
          </CardContent>
        </Card>
      )}

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

export function EventCard() {
  return (
    <Card>
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
              Sport
            </Badge>

            <Badge variant="outline">
              <Users className="mr-1 h-3 w-3" />
              broj igraca
            </Badge>

          </div>

          <div className="ml-auto [grid-area:loc] w-full">
            <div className="flex flex-nowrap items-center gap-2 h-full justify-center">
              <MapPin className="mr-1 h-5 w-5" />
              <div className="md:text-lg">
                lokacija terena
              </div>
            </div>
          </div>

          <div className="text-center text-2xl md:text-5xl md:font-thin [grid-area:dist]">
            2 km
          </div>

          <h2 className="text-lg md:text-2xl font-semibold [grid-area:name]">
            Naziv eventa
          </h2>

          <p className="rounded-md bg-muted p-3 text-sm text-muted-foreground [grid-area:desc]">
            Opis (prvih n znakova)
          </p>

        </div>
      </CardContent>
    </Card>
  );
}
