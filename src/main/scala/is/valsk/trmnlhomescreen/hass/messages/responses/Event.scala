package is.valsk.trmnlhomescreen.hass.messages.responses

import is.valsk.trmnlhomescreen.hass.EntityState
import is.valsk.trmnlhomescreen.hass.messages.{HassIdentifiableMessage, HassResponseMessage}
import zio.json.{DeriveJsonDecoder, JsonDecoder, jsonField}

case class Event(
    id: Int,
    `type`: String,
    data: EventData,
) extends HassResponseMessage
    with HassIdentifiableMessage

case class EventData(
    @jsonField("new_state")
    newState: EntityState,
)

object Event {
  given JsonDecoder[Event] = DeriveJsonDecoder.gen[Event]
  given JsonDecoder[EventData] = DeriveJsonDecoder.gen[EventData]
}
