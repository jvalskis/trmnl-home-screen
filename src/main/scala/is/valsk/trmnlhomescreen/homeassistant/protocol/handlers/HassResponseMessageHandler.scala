package is.valsk.trmnlhomescreen.homeassistant.protocol.handlers

import is.valsk.trmnlhomescreen.homeassistant.message.Type
import HassResponseMessageHandler.PartialHassResponseMessageHandler
import is.valsk.trmnlhomescreen.homeassistant.message.model.HassResponseMessage
import zio.*
import zio.http.WebSocketChannel

trait HassResponseMessageHandler {

  def get: PartialHassResponseMessageHandler
}

object HassResponseMessageHandler {

  type PartialHassResponseMessageHandler = PartialFunction[HassResponseMessageContext, Task[Unit]]

  val empty: PartialHassResponseMessageHandler = PartialFunction.empty[HassResponseMessageContext, Task[Unit]]

  case class HassResponseMessageContext(
      channel: WebSocketChannel,
      message: HassResponseMessage,
  )

}
