package is.valsk.trmnlhomescreen.hass.messages.responses

import is.valsk.trmnlhomescreen.hass.EntityState
import is.valsk.trmnlhomescreen.hass.messages.{HassIdentifiableMessage, HassResponseMessage}
import zio.json.{DeriveJsonDecoder, JsonDecoder, jsonField}

case class Event(
    id: Int,
    `type`: String,
    event: EventPayload,
) extends HassResponseMessage
    with HassIdentifiableMessage

case class EventPayload(
    data: StateChangedData,
)

case class StateChangedData(
    @jsonField("new_state")
    newState: EntityState,
)

object Event {
  given JsonDecoder[StateChangedData] = DeriveJsonDecoder.gen[StateChangedData]
  given JsonDecoder[EventPayload] = DeriveJsonDecoder.gen[EventPayload]
  given JsonDecoder[Event] = DeriveJsonDecoder.gen[Event]
}
