package is.valsk.trmnlhomescreen.hass.messages.commands

import is.valsk.trmnlhomescreen.hass.messages.{HassIdentifiableMessage, HassRequestMessage, Type}
import zio.json.{DeriveJsonEncoder, JsonEncoder}

case class WsCommand(
    `type`: String,
    id: Int
) extends HassRequestMessage with HassIdentifiableMessage

object WsCommand {
  given JsonEncoder[WsCommand] = DeriveJsonEncoder.gen[WsCommand]

}

object AreaRegistryListCommand {

  def apply(id: Int): WsCommand = WsCommand("config/area_registry/list", id)
}