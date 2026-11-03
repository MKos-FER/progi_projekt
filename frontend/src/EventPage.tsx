import {useParams} from "react-router-dom";

export default function EventPage() {
  const { eventId } = useParams();

  return (
    <>
      event: {eventId}
    </>
  )
}