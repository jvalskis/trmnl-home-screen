package is.valsk.trmnlhomescreen.homeassistant.protocol.handlers

import is.valsk.trmnlhomescreen.homeassistant.message.{RequestRepository, Type}
import HassResponseMessageHandler.{HassResponseMessageContext, PartialHassResponseMessageHandler}
import is.valsk.trmnlhomescreen.homeassistant.message.model.{HassIdentifiableMessage, HassResponseMessage}
import zio.*

class ResultHandler(
    requestRepository: RequestRepository,
    homeAssistantResultHandlers: Map[Type, HomeAssistantResultHandler],
) extends HassResponseMessageHandler {

  override def get: PartialHassResponseMessageHandler = {
    case HassResponseMessageContext(channel, result: (HassResponseMessage & HassIdentifiableMessage)) =>
      for {
        messageType <- requestRepository.get(result.id)
        handler = messageType.flatMap(homeAssistantResultHandlers.get)
        _ <- ZIO.foreachDiscard(handler)(_.handle(channel, result))
      } yield ()
  }

}

object ResultHandler {

  val layer: URLayer[RequestRepository & Seq[HomeAssistantResultHandler], ResultHandler] = ZLayer {
    for {
      requestRepository <- ZIO.service[RequestRepository]
      handlers <- ZIO.service[Seq[HomeAssistantResultHandler]]
    } yield ResultHandler(requestRepository, handlers.map(h => h.getSupportedType -> h).toMap)
  }

}
