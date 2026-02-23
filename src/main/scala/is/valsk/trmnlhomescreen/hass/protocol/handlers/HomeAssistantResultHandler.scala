package is.valsk.trmnlhomescreen.hass.protocol.handlers

import is.valsk.trmnlhomescreen.hass.messages.{HassIdentifiableMessage, HassResponseMessage, Type}
import is.valsk.trmnlhomescreen.hass.protocol.api.RequestRepository
import zio.http.WebSocketChannel
import zio.{Task, ZIO}

trait HomeAssistantResultHandler {

  def handle(channel: WebSocketChannel, result: HassResponseMessage & HassIdentifiableMessage): Task[Unit] = {
    ZIO.logInfo(s"Received message: $result") *>
    requestRepository.get(result.id).flatMap {
      case Some(t) if t == supportedType =>
        handleInternal(channel, result)
      case _ =>
        ZIO.logWarning(s"No handler found for message: $result")
    }

  }

  protected def handleInternal(
      channel: WebSocketChannel,
      result: HassResponseMessage & HassIdentifiableMessage,
  ): Task[Unit]

  protected def requestRepository: RequestRepository

  protected val supportedType: Type

}
