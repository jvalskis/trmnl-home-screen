package is.valsk.trmnlhomescreen.homeassistant.message.model.commands

import is.valsk.trmnlhomescreen.homeassistant.message.Type.{GetStates, SubscribeEvents}
import is.valsk.trmnlhomescreen.homeassistant.message.model.{HassIdentifiableMessage, HassRequestMessage}
import is.valsk.trmnlhomescreen.homeassistant.message.Type
import zio.json.{DeriveJsonEncoder, JsonEncoder, jsonField}

case class WsCommand(
    `type`: String,
    id: Int,
    @jsonField("event_type")
    eventType: Option[String] = None,
) extends HassRequestMessage
    with HassIdentifiableMessage

object WsCommand {
  given JsonEncoder[WsCommand] = DeriveJsonEncoder.gen[WsCommand]

}

object SubscribeEventsCommand {

  def apply(): WsCommand = WsCommand(SubscribeEvents.typeName, 0, eventType = Some("state_changed"))
}

object GetStatesCommand {

  def apply(): WsCommand = WsCommand(GetStates.typeName, 0)
}
