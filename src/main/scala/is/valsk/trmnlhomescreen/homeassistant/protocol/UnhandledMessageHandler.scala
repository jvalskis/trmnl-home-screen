package is.valsk.trmnlhomescreen.homeassistant.protocol

import is.valsk.trmnlhomescreen.homeassistant.protocol.ChannelHandler.PartialChannelHandler
import zio.*

class UnhandledMessageHandler extends ChannelHandler {

  override def get: PartialChannelHandler = {
    case message => ZIO.logInfo(s"Unhandled: $message")
  }
}

object UnhandledMessageHandler {

  val layer: ULayer[UnhandledMessageHandler] = ZLayer.succeed(UnhandledMessageHandler())
}
