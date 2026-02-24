package is.valsk.trmnlhomescreen.homeassistant.message

import is.valsk.trmnlhomescreen.homeassistant.message.model.commands.WsCommand
import zio.http.ChannelEvent.Read
import zio.http.{WebSocketChannel, WebSocketFrame}
import zio.json.EncoderOps
import zio.{Task, URLayer, ZIO, ZLayer}

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
        messageType <- ZIO.fromEither(Type.parse(message.`type`))
        _ <- requestTypeRepository.add(id, messageType)
        _ <- ZIO.logInfo(s"Sending message $json")
        _ <- channel.send(Read(WebSocketFrame.text(json)))
      } yield ()

  }

  val layer: URLayer[MessageIdGenerator & RequestRepository, MessageSender] = ZLayer {
    for {
      messageIdGenerator <- ZIO.service[MessageIdGenerator]
      requestTypeRepository <- ZIO.service[RequestRepository]
    } yield MessageSenderLive(messageIdGenerator, requestTypeRepository)
  }

}
