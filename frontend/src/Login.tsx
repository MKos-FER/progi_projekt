import { Card, CardContent, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Link } from "react-router-dom";
import { useSearchParams } from "react-router-dom";
import {Alert, AlertDescription, AlertTitle} from "@/components/ui/alert.tsx";
import {Separator} from "@/components/ui/separator.tsx";
import {CircleX} from "lucide-react"

export default function Login() {

  const [searchParams] = useSearchParams();

  const error = searchParams.get("loginError");

  function HandleGoogleLogin(){
  window.location.href = "/oauth2/authorization/google"
  }

  return (


    <>
      <div>

      { error != null ? (
        <div className="my-4 mx-auto flex flex-col gap-8">
        <Alert className="text-destructive *:[svg]:text-current bg-destructive-foreground">
          <CircleX/>
          <AlertTitle>Dogodila se greška prilikom autorizacije </AlertTitle>
          <AlertDescription>
            Prilikom autorizacije došlo je do progreške. Pokušajte ponovno.
            <Separator className={"my-3"}/>
           
          </AlertDescription>
        </Alert>
      </div>
      ) : (
        <Card className="absolute left-1/2 top-[calc(50%+45px)] md:h-[480px] md:w-[450px] h-90 w-80 -translate-x-1/2 -translate-y-1/2">
        <CardContent className="flex flex-col gap-6 p-7 h-full items-center justify-center">
          <CardTitle className="md:text-4xl text-4xl font-bold ">
            Dobrodošli!
          </CardTitle>
          
          <Button className="w-full text-lg md:h-14 h-12" onClick={HandleGoogleLogin}>
            Prijava s OAUTH2
          </Button>
         

          <Link to="/" className="w-full md:h-14 h-12 ">
            <Button className="w-full md:h-14 h-12 " variant="outline">
              Nastavi kao gost
            </Button>
          </Link>
        </CardContent>
      </Card>)}
      </div>

    </>
  );
}

