import { BrowserRouter, Route, Routes, NavLink, Link, useNavigate } from "react-router-dom";

import './styles/App.css'
import Home from './HomePage.tsx'
import Login from './Login.tsx'
import ReportsPage from './ReportsPage.tsx'
import MyEvents from './MyEventsPage.tsx'
import EventPage from './EventPage.tsx'
import { CookiePolicy, PrivacyPolicy } from './LegalStuff.tsx'
import EditProfile from './EditProfilePage.tsx'
import OnBoarding from './OnBoardingPage.tsx'


import { Button } from "@/components/ui/button.tsx";
import {
  NavigationMenu,
  NavigationMenuItem,
  NavigationMenuList
} from "@/components/ui/navigation-menu";

import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuGroup,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";

import {
  Menu,
  DoorOpen,
  UserPen,
} from "lucide-react";

import letsDoSportsLogo from "./assets/logo.svg"

import { Avatar, AvatarFallback, AvatarImage } from "@/components/ui/avatar";
import { useUserProfile, useUserLogout, useIsAuth } from "./apiStore/useUsers.ts";
import { getDefaultAvatar } from "./lib/utils.ts";
import {type JSX} from "react";


function App() {
  return (
    <div className="flex flex-col bg-background p-4 h-svh overflow-visible">
      <BrowserRouter>
        <NavigationBar />

        <div className="h-full">
          <Routes>
            <Route path="/" element={<Home />} />
            <Route path="/legal/cookie_policy" element={<CookiePolicy />} />
            <Route path="/legal/privacy_policy" element={<PrivacyPolicy />} />
            <Route path="/reports" element={<ReportsPage />} />
            <Route path="/events" element={<MyEvents />} />
            <Route path="/events/:eventId" element={<EventPage />} />
            <Route path="/login" element={<Login />} />
            <Route path="/profile/edit" element={<EditProfile />} />
            <Route path="/register" element={<OnBoarding />} />
          </Routes>
        </div>
      </BrowserRouter>
    </div>
  )
}


type NavLink = {
  url: string;
  name: string;
}
function NavigationBar() {
  // const [isAuth, setIsAuth] = useState(false)

  const { data: user, isLoading } = useUserProfile()
  const { mutate: doLogout, isPending } = useUserLogout()

  const { data: isAuth } = useIsAuth();

  // useEffect(() => {
  //   async function checkAuth(): Promise<boolean> {
  //     const result = await fetch("/api/v1/users/me");
  //
  //     if (result.status === 401) {
  //       return false;
  //     } else {
  //       return true;
  //     }
  //
  //   }
  //
  //   checkAuth().then(val => setIsAuth(val)).catch(() => {});
  // }, [isPending]);

  const navLinks: Array<NavLink> = [
    { url: "/", name: "Početna" },
    { url: "/events", name: "Moji događaji" },
    { url: "/reports", name: "Moje prijave" }
  ];
  const navigate = useNavigate();
  let profile_button: JSX.Element;

  if (isAuth && !isLoading && user && !isPending) {
    profile_button = (
      <DropdownMenu >
        <DropdownMenuTrigger nativeButton={false} render={
          <Avatar size={"lg"}>
            <AvatarImage src={getDefaultAvatar(user.profilePhotoUrl) ?? ""} />
            <AvatarFallback className="text-2xl">{user.name[0]}</AvatarFallback>
          </Avatar>
        } />
        <DropdownMenuContent className="w-80 p-1 block grid-cols-2" align="start">
          <div className="grid w-full p-2
            grid-cols-[3fr_1fr]
            grid-rows-[75px_1fr_1fr]
            [grid-template-areas:'name_avatar'_'view_view'_'logout_logout']
            ">
            <div className="flex flex-col">
              <span className="text-lg">{user.name}</span>
              <span className="text-muted-foreground text-md">{user.username}</span>
            </div>
            <Avatar className="self-center justify-self-end h-full w-full ">
              <AvatarImage src={getDefaultAvatar(user.profilePhotoUrl) ?? ""} />
              <AvatarFallback className="text-4xl">{user.name[0]}</AvatarFallback>
            </Avatar>

            <DropdownMenuItem className="[grid-area:view] mt-2 text-md self-center" onClick={() => void navigate("/profile/edit")}>
              <UserPen />
              Edit user profile
            </DropdownMenuItem>

            <DropdownMenuItem className="[grid-area:logout] text-md border-transparent focus:border-b focus:border-t focus:border-b-destructive text-destructive" onClick={() => doLogout()}>
              <DoorOpen />
              <p>Logout</p>
            </DropdownMenuItem>
          </div>
        </DropdownMenuContent>
      </DropdownMenu>
    );
  } else {
    profile_button = (
      <Button className="py-5" onClick={() => void navigate("/login")}>
        PRIJAVA
      </Button>
    );
  }


  return (
    <header className="flex flex-wrap items-center gap-4">

      <Link to="/">
        <img
          src={letsDoSportsLogo}
          alt="Let's Do Sports Logo"
          width="50"
          className="dark:brightness-20 grayscale-90 hover:grayscale-0"
        />
      </Link>

      <PcNavMenu navLinks={navLinks} />
      <PhoneNavMenu navLinks={navLinks} />

      {profile_button}

    </header>
  );
}

function PhoneNavMenu({ navLinks }: { navLinks: NavLink[] }) {
  const navMenuItems: JSX.Element[] = navLinks.map(link => {
    return (
      <NavLink key={link.url}
        to={link.url}
      >
        <DropdownMenuItem className="navigation-bar-button">
          {link.name}
        </DropdownMenuItem>
      </NavLink>
    );
  })

  return (
    <>
      <div className="max-w-none block md:hidden flex-1 justify-start
                             rounded-md bg-card px-4 py-2">
      </div>
      <DropdownMenu >
        <DropdownMenuTrigger render={<button className="block md:hidden"><Menu /></button>} />
        <DropdownMenuContent className="w-40 p-1 block md:hidden" align="start">
          <DropdownMenuGroup>
            <DropdownMenuLabel>Odaberi prikaz</DropdownMenuLabel>
            {navMenuItems}
          </DropdownMenuGroup>
        </DropdownMenuContent>
      </DropdownMenu>
    </>
  );
}

function PcNavMenu({ navLinks }: { navLinks: NavLink[] }) {
  const navMenuItems: JSX.Element[] = navLinks.map(link => {
    return (
      <NavigationMenuItem key={link.url}>
        <NavLink
          to={link.url}
          className="navigation-bar-button rounded-sm p-1"
        >
          {link.name}
        </NavLink>
      </NavigationMenuItem>
    );
  })

  return (
    <NavigationMenu className="max-w-none hidden md:block flex-1 justify-start
                             rounded-md border bg-card px-4 py-2">
      <NavigationMenuList className="flex-wrap gap-4">
        {navMenuItems}
      </NavigationMenuList>
    </NavigationMenu>
  );
}

export default App
