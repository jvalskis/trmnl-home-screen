package is.valsk.trmnlhomescreen.hass.protocol

import is.valsk.trmnlhomescreen.hass.messages.{MessageIdGenerator, Type}
import is.valsk.trmnlhomescreen.hass.messages.commands.WsCommand
import is.valsk.trmnlhomescreen.hass.protocol.api.RequestRepository
import zio.http.ChannelEvent.Read
import zio.{Task, URLayer, ZIO, ZLayer}
import zio.http.{WebSocketChannel, WebSocketFrame}
import zio.json.EncoderOps

trait MessageSender {
  def send(message: WsCommand)(using channel: WebSocketChannel): Task[Unit]
}

object MessageSender {

  private class MessageSenderLive(
    messageIdGenerator: MessageIdGenerator,
    requestTypeRepository: RequestRepository,
  ) extends MessageSender {

    def send(message: WsCommand)(using channel: WebSocketChannel): Task[Unit] =
      for {
        id <- messageIdGenerator.generate()
        json = message.copy(id = id).toJson
        _ <- requestTypeRepository.add(id, Type.valueOf(message.`type`))
        _ <- ZIO.logInfo(s"Sending message $json") *>
          channel.send(Read(WebSocketFrame.text(json)))
      } yield ()

  }

  val layer: URLayer[MessageIdGenerator & RequestRepository, MessageSender] = ZLayer {
    for {
      messageIdGenerator <- ZIO.service[MessageIdGenerator]
      requestTypeRepository <- ZIO.service[RequestRepository]
    } yield MessageSenderLive(messageIdGenerator, requestTypeRepository)
  }

}
