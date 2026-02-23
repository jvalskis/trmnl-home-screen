package is.valsk.trmnlhomescreen.hass.messages.commands

import is.valsk.trmnlhomescreen.hass.messages.Type.{GetStates, SubscribeEntities}
import zio.json.*
import is.valsk.trmnlhomescreen.hass.messages.{HassIdentifiableMessage, HassRequestMessage, Type}
import zio.json.{DeriveJsonEncoder, JsonEncoder}

case class WsCommand(
    `type`: String,
    id: Int,
    @jsonField("entity_ids")
    entityIds: Seq[String] = Seq.empty,
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

object SubscribeEntitiesCommand {

  def apply(entityIds: Seq[String]): WsCommand = WsCommand(SubscribeEntities.typeName, 0, entityIds)
}

object GetStatesCommand {

  def apply(): WsCommand = WsCommand(GetStates.typeName, 0)
}
