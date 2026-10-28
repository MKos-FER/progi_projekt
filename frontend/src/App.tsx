import {BrowserRouter, Route, Routes, NavLink} from "react-router-dom";

import './styles/App.css'
import Home from './Home.tsx'
import Login from './Login.tsx'
import Profile from './Profile.tsx'


function App() {
  return (
    <div>
      <BrowserRouter>
        <NavigationBar/>

        <Routes>
          <Route path="/" element={<Home />} />
          <Route path="/profile" element={<Profile />} />
          <Route path="/login" element={<Login />} />
        </Routes>
      </BrowserRouter>
    </div>
  )
}

function NavigationBar() {
  return (
    <div className="navigation-bar">
      <NavLink className="navigation-bar-button" to="/">Home</NavLink>
      <NavLink className="navigation-bar-button" to="/profile">Profile</NavLink>
      <NavLink className="navigation-bar-button" to="/login">Login</NavLink>
    </div>
  )
}

export default App
