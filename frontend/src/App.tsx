import { useState } from 'react'

import footballField from './assets/football-field.svg'
import './styles/App.css'


function App() {
  const [count, setCount] = useState(0)

  return (
    <>
      <section></section>
      <section id="spacer-top"></section>
      <div className="ticks"></div>

      <section id="center">
        <div className="hero">
          <img src={footballField} width={200} alt="Vite logo" />
        </div>
        <div>
          <h1>Get started</h1>
          <p>
            Run <code>bun dev</code> to start the server and edit <code>src/App.tsx</code> to see some changes
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
