import {BrowserRouter, Route, Routes, NavLink, Link, useNavigate} from "react-router-dom";

import './styles/App.css'
import Home from './HomePage.tsx'
import Login from './Login.tsx'
import ReportsPage from './ReportsPage.tsx'
import MyEvents from './MyEventsPage.tsx'
import EventPage from './EventPage.tsx'
import { CookiePolicy, PrivacyPolicy } from './LegalStuff.tsx'
import EditProfile from './EditProfilePage.tsx'


import {Button} from "@/components/ui/button.tsx";
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
} from "lucide-react";

import type {JSX} from "react";


function App() {
  return (
    <div className="bg-background p-4">
      <BrowserRouter>
        <NavigationBar/>

        <Routes>
          <Route path="/" element={<Home />} />
          <Route path="/legal/cookie_policy" element={<CookiePolicy />} />
          <Route path="/legal/privacy_policy" element={<PrivacyPolicy />} />
          <Route path="/reports" element={<ReportsPage />} />
          <Route path="/events" element={<MyEvents />} />
          <Route path="/events/:eventId" element={<EventPage />} />
          <Route path="/login" element={<Login />} />
          <Route path="/profile/edit" element={<EditProfile/>}/>
        </Routes>
      </BrowserRouter>
    </div>
  )
}


type NavLink = {
  url: string;
  name: string;
}
function NavigationBar() {
  const navLinks: Array<NavLink> = [
    {url:"/events", name:"Moji događaji"},
    {url:"/reports", name:"Moje prijave"}
  ];
  const navigate = useNavigate();
  return (
    <header className="flex flex-wrap items-center gap-4">

      <Link to="/">
        <div className="flex h-16 w-44 shrink-0 items-center justify-center
                        rounded-md border bg-card">
        <span className="text-xl font-semibold">
          LOGO pic
        </span>
        </div>
      </Link>

      <PcNavMenu navLinks={navLinks} />
      <PhoneNavMenu navLinks={navLinks} />

      <Button className="py-5" onClick={() => void navigate("/login")}>
        PRIJAVA
      </Button>

    </header>
  );
}

function PhoneNavMenu({navLinks}: {navLinks: NavLink[]}) {
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
        <DropdownMenuTrigger render={<Menu className="block md:hidden" />} />
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

function PcNavMenu({navLinks}: {navLinks: NavLink[]}) {
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
