package is.valsk.trmnlhomescreen.hass.protocol

import is.valsk.trmnlhomescreen.hass.protocol.ChannelHandler.PartialChannelHandler
import zio.*
import zio.http.*

trait ChannelHandler {
  def get: PartialChannelHandler
}

object ChannelHandler {

  type PartialChannelHandler = PartialFunction[(WebSocketChannel, WebSocketChannelEvent), Task[Unit]]

  val empty: PartialChannelHandler = PartialFunction.empty[(WebSocketChannel, WebSocketChannelEvent), Task[Unit]]
}