package is.valsk.trmnlhomescreen.homeassistant.protocol.handlers

import is.valsk.trmnlhomescreen.homeassistant.message.model.{HassIdentifiableMessage, HassResponseMessage}
import is.valsk.trmnlhomescreen.homeassistant.message.{RequestRepository, Type}
import zio.http.WebSocketChannel
import zio.{Task, ZIO}

class HomeAssistantResultHandler(
    requestRepository: RequestRepository,
    handler: PartialFunction[(WebSocketChannel, HassResponseMessage & HassIdentifiableMessage, Type), Task[Unit]],
    supportedType: Type,
) {

  def handle(channel: WebSocketChannel, result: HassResponseMessage & HassIdentifiableMessage): Task[Unit] = {
    ZIO.logInfo(s"Received message: $result") *>
      requestRepository.get(result.id).flatMap {
        case Some(t) if t == supportedType && handler.isDefinedAt(channel, result, t) =>
          handler(channel, result, t)
        case _ =>
          ZIO.logWarning(s"No handler found for message: $result")
      }

  }

  def getSupportedType: Type = supportedType
}
