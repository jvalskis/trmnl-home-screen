package is.valsk.trmnlhomescreen.hass.messages.commands

import is.valsk.trmnlhomescreen.hass.messages.Type.{GetStates, SubscribeEvents}
import zio.json.*
import is.valsk.trmnlhomescreen.hass.messages.{HassIdentifiableMessage, HassRequestMessage, Type}
import zio.json.{DeriveJsonEncoder, JsonEncoder}

case class WsCommand(
    `type`: String,
    id: Int,
    @jsonField("entity_ids")
    entityIds: Option[Seq[String]] = None,
    @jsonField("event_type")
    eventType: Option[String] = None,
) extends HassRequestMessage
    with HassIdentifiableMessage

object WsCommand {
  given JsonEncoder[WsCommand] = DeriveJsonEncoder.gen[WsCommand]

}

object AreaRegistryListCommand {

  def apply(id: Int): WsCommand = WsCommand("config/area_registry/list", id)
}

object EntityRegistryListCommand {

  def apply(id: Int): WsCommand = WsCommand("config/entity_registry/list", id)
}

object DeviceRegistryListCommand {

  def apply(id: Int): WsCommand = WsCommand("config/device_registry/list", id)
}

object SubscribeEventsCommand {

  def apply(entityIds: Seq[String]): WsCommand = WsCommand(SubscribeEvents.typeName, 0, eventType = Some("state_changed"))
}

object GetStatesCommand {

  def apply(): WsCommand = WsCommand(GetStates.typeName, 0)
}
