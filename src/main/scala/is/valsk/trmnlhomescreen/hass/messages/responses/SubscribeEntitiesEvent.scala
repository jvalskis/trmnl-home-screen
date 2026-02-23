package is.valsk.trmnlhomescreen.hass.messages.responses

import is.valsk.trmnlhomescreen.hass.EntityAttributes
import is.valsk.trmnlhomescreen.hass.messages.{HassIdentifiableMessage, HassResponseMessage}
import zio.json.{DeriveJsonDecoder, JsonDecoder, jsonField}

case class SubscribeEntitiesEvent(
    `type`: String,
    id: Int,
    event: SubscribeEntitiesEventData,
) extends HassResponseMessage with HassIdentifiableMessage

case class SubscribeEntitiesEventData(
    @jsonField("a")
    added: Option[Map[String, CompressedEntityState]],
    @jsonField("c")
    changed: Option[Map[String, CompressedEntityStateChange]],
    @jsonField("r")
    removed: Option[List[String]],
)

case class CompressedEntityState(
    @jsonField("s")
    state: String,
    @jsonField("a")
    attributes: EntityAttributes,
    @jsonField("c")
    context: String,
    @jsonField("lc")
    lastChanged: Double,
    @jsonField("lu")
    lastUpdated: Double,
)

case class CompressedEntityStateChange(
    @jsonField("+")
    diff: CompressedEntityStateDiff,
)

case class CompressedEntityStateDiff(
    @jsonField("s")
    state: Option[String],
    @jsonField("a")
    attributes: Option[EntityAttributes],
    @jsonField("c")
    context: Option[String],
    @jsonField("lc")
    lastChanged: Option[Double],
    @jsonField("lu")
    lastUpdated: Option[Double],
)

object SubscribeEntitiesEvent {
  given JsonDecoder[CompressedEntityStateDiff] = DeriveJsonDecoder.gen[CompressedEntityStateDiff]
  given JsonDecoder[CompressedEntityStateChange] = DeriveJsonDecoder.gen[CompressedEntityStateChange]
  given JsonDecoder[CompressedEntityState] = DeriveJsonDecoder.gen[CompressedEntityState]
  given JsonDecoder[SubscribeEntitiesEventData] = DeriveJsonDecoder.gen[SubscribeEntitiesEventData]
  given JsonDecoder[SubscribeEntitiesEvent] = DeriveJsonDecoder.gen[SubscribeEntitiesEvent]
}
