package is.valsk.trmnlhomescreen.homeassistant.message.model.responses

import is.valsk.trmnlhomescreen.homeassistant.message.model.responses.Event.EventPayload
import is.valsk.trmnlhomescreen.homeassistant.message.model.{HassIdentifiableMessage, HassResponseMessage}
import zio.json.{DeriveJsonDecoder, JsonDecoder, jsonField}

case class Event(
    id: Int,
    `type`: String,
    event: EventPayload,
) extends HassResponseMessage
    with HassIdentifiableMessage

object Event {
  given JsonDecoder[StateChangedData] = DeriveJsonDecoder.gen[StateChangedData]
  given JsonDecoder[EventPayload] = DeriveJsonDecoder.gen[EventPayload]
  given JsonDecoder[Event] = DeriveJsonDecoder.gen[Event]

  case class EventPayload(
      data: StateChangedData,
  )

  case class StateChangedData(
      @jsonField("new_state")
      newState: EntityState,
  )

  final case class EntityAttributes(
      @jsonField("friendly_name")
      friendlyName: Option[String],
      @jsonField("unit_of_measurement")
      unitOfMeasurement: Option[String],
      @jsonField("device_class")
      deviceClass: Option[String],
  )

  object EntityAttributes:
    given JsonDecoder[EntityAttributes] = DeriveJsonDecoder.gen[EntityAttributes]

  final case class EntityState(
      @jsonField("entity_id")
      entityId: String,
      state: String,
      attributes: EntityAttributes,
  )

  object EntityState:
    given JsonDecoder[EntityState] = DeriveJsonDecoder.gen[EntityState]

}
