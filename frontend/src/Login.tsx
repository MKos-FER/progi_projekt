import { Card, CardContent, CardTitle, CardAction } from "@/components/ui/card";
import { Button } from "@/components/ui/button";

export default function Login() {
  return (
    <>
      
  <Card className="absolute left-1/2 top-[calc(50%+45px)] md:h-[480px] md:w-[450px] h-90 w-80 -translate-x-1/2 -translate-y-1/2">
  <CardContent className="flex flex-col gap-6 p-7 h-full items-center justify-center">
    <CardTitle className="md:text-5xl text-4xl font-bold ">
      Dobrodošli!
    </CardTitle>

    <Button className="w-full text-lg md:h-14 h-12">
      Prijava s OAUTH2
    </Button>

    <Button className="w-full md:h-14 h-12 " variant="outline">
      Nastavi kao gost
    </Button>
  </CardContent>
</Card>

    </>
  )
}

