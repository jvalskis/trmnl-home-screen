package is.valsk.trmnlhomescreen.homeassistant.protocol

import is.valsk.trmnlhomescreen.homeassistant.message.MessageParser
import is.valsk.trmnlhomescreen.homeassistant.message.model.HassResponseMessage
import is.valsk.trmnlhomescreen.homeassistant.protocol.ChannelHandler.PartialChannelHandler
import is.valsk.trmnlhomescreen.homeassistant.protocol.handlers.HassResponseMessageHandler.{HassResponseMessageContext, PartialHassResponseMessageHandler}
import is.valsk.trmnlhomescreen.homeassistant.protocol.handlers.HassResponseMessageHandler
import zio.*
import zio.http.ChannelEvent.*
import zio.http.*

class TextHandler(
    handleHassMessages: PartialHassResponseMessageHandler,
    messageParser: MessageParser[HassResponseMessage],
) extends ChannelHandler {

  override def get: PartialChannelHandler = { case (channel, Read(WebSocketFrame.Text(json))) =>
    val result = for {
      _ <- ZIO.logDebug(s"Received message: $json")
      parsedMessage <- messageParser.parseMessage(json)
      _ <- handleHassMessages(HassResponseMessageContext(channel, parsedMessage))
    } yield ()
    result
      .catchAll(e => ZIO.logError(s"Failed to parse message: ${e.getMessage}. Message: $json"))
  }

}

object TextHandler {

  private val rest: HassResponseMessageHandler = new HassResponseMessageHandler {
    override def get: PartialHassResponseMessageHandler = { case HassResponseMessageContext(_, message) =>
      ZIO.logWarning(s"Message not handled: $message")
    }
  }

  val layer: URLayer[List[HassResponseMessageHandler] & MessageParser[HassResponseMessage], TextHandler] = ZLayer {
    for {
      handlers <- ZIO.service[List[HassResponseMessageHandler]]
      parser <- ZIO.service[MessageParser[HassResponseMessage]]
      combinedHandler = (handlers :+ rest)
        .foldLeft(HassResponseMessageHandler.empty) { (a, b) => a orElse b.get }
    } yield TextHandler(combinedHandler, parser)
  }

}
