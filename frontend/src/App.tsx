import { useState } from 'react'
import heroImg from './assets/hero.png'
import reactLogo from './assets/react.svg'
import viteLogo from './assets/vite.svg'
import './App.css'


function App() {
  const [count, setCount] = useState(0)

  return (
    <>
      <section></section>
      <section id="spacer-top"></section>
      <div className="ticks"></div>

      <section id="center">
        <div className="hero">
          <img src={heroImg} className="base" width="170" height="179" alt="" />
          <img src={reactLogo} className="framework" alt="React logo" />
          <img src={viteLogo} className="vite" alt="Vite logo" />
        </div>
        <div>
          <h1>Get started</h1>
          <p>
            Edit <code>src/App.tsx</code> and save to test <code>HMR</code>
          </p>
        </div>
        <button
          type="button"
          className="counter"
          tabIndex={0}
          onClick={() => setCount((count) => count + 1)}
        >
          Count is {count}
        </button>
      </section>


      <div className="ticks"></div>
      <section id="spacer-bottom"></section>
    </>
  )
}

export default App
