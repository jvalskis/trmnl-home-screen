package is.valsk.trmnlhomescreen.hass.protocol.api

import is.valsk.trmnlhomescreen.hass.messages.MessageIdGenerator
import is.valsk.trmnlhomescreen.hass.messages.commands.{AreaRegistryListCommand, SubscribeEntitiesCommand, WsCommand}
import is.valsk.trmnlhomescreen.hass.messages.responses.AuthOK
import is.valsk.trmnlhomescreen.hass.protocol.api.HassResponseMessageHandler.{HassResponseMessageContext, PartialHassResponseMessageHandler}
import zio.*
import zio.http.ChannelEvent.Read
import zio.http.*
import zio.json.*

class ConnectHandler(
    messageSender: MessageSender,
) extends HassResponseMessageHandler {

  override def get: PartialHassResponseMessageHandler = { case HassResponseMessageContext(channel, _: AuthOK) =>
    messageSender.send(SubscribeEntitiesCommand(Seq("entity.id")))(using channel)
  }

}

object ConnectHandler {

  val layer: URLayer[MessageSender, ConnectHandler] = ZLayer {
    for {
      messageSender <- ZIO.service[MessageSender]
    } yield ConnectHandler(messageSender)
  }

}

trait MessageSender {
  def send(message: WsCommand)(using channel: WebSocketChannel): Task[Unit]
}

object MessageSender {

  private class MessageSenderLive(
      messageIdGenerator: MessageIdGenerator,
  ) extends MessageSender {

    def send(message: WsCommand)(using channel: WebSocketChannel): Task[Unit] =
      for {
        id <- messageIdGenerator.generate()
        json = message.copy(id = id).toJson
        _ <- ZIO.logInfo(s"Sending message $json") *>
          channel.send(Read(WebSocketFrame.text(json)))
      } yield ()

  }

  val layer: URLayer[MessageIdGenerator, MessageSender] = ZLayer {
    for {
      messageIdGenerator <- ZIO.service[MessageIdGenerator]
    } yield MessageSenderLive(messageIdGenerator)
  }

}
