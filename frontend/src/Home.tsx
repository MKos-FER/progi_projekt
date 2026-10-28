import './styles/Home.css'
import {useState} from "react";


export default function Home() {
  const [count, setCount] = useState(0);

  let buttonText = "A clicky button";
  if (count > 0) {
    buttonText = `A clicky button you pressed ${count} times`;
  }

  return (
    <>
      <div>Main page</div>
      <br/>
      <button className="counter" onClick={() => {setCount(count + 1)}}>{buttonText}</button>
    </>
  )
}
