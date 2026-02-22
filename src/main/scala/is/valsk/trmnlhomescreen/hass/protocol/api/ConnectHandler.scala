package is.valsk.trmnlhomescreen.hass.protocol.api

import is.valsk.trmnlhomescreen.hass.messages.MessageIdGenerator
import is.valsk.trmnlhomescreen.hass.messages.commands.{AreaRegistryListCommand, WsCommand}
import is.valsk.trmnlhomescreen.hass.messages.responses.AuthOK
import is.valsk.trmnlhomescreen.hass.protocol.api.HassResponseMessageHandler.{HassResponseMessageContext, PartialHassResponseMessageHandler}
import zio.*
import zio.http.ChannelEvent.Read
import zio.http.*
import zio.json.*

class ConnectHandler(
    messageIdGenerator: MessageIdGenerator,
) extends HassResponseMessageHandler {

  override def get: PartialHassResponseMessageHandler = {
    case HassResponseMessageContext(channel, _: AuthOK) =>
      for {
        messageId <- messageIdGenerator.generate()
        json = AreaRegistryListCommand(messageId).toJson
        _ <- ZIO.logInfo(s"Sending message $json")
        _ <- channel.send(Read(WebSocketFrame.text(json)))
      } yield ()
  }
}

object ConnectHandler {

  val layer: URLayer[MessageIdGenerator, ConnectHandler] = ZLayer {
    for {
      messageIdGenerator <- ZIO.service[MessageIdGenerator]
    } yield ConnectHandler(messageIdGenerator)
  }
}