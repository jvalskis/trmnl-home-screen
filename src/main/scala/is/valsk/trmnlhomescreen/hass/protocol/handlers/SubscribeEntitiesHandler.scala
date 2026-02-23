package is.valsk.trmnlhomescreen.hass.protocol.handlers

import is.valsk.trmnlhomescreen.hass.messages.responses.*
import HassResponseMessageHandler.{HassResponseMessageContext, PartialHassResponseMessageHandler}
import is.valsk.trmnlhomescreen.hass.messages.{HassIdentifiableMessage, HassResponseMessage, Type}
import is.valsk.trmnlhomescreen.hass.protocol.api.{EntityStateRepository, RequestRepository}
import zio.*
import zio.http.WebSocketChannel

class SubscribeEntitiesHandler(
    entityStateRepository: EntityStateRepository,
    val requestRepository: RequestRepository,
) extends HomeAssistantResultHandler {

  override def handleInternal(
      channel: WebSocketChannel,
      result: HassResponseMessage & HassIdentifiableMessage,
  ): Task[Unit] =
    result match {
      case result: Event =>
        val entityState = result.data.newState
        entityStateRepository.add(entityState.entityId, entityState) *>
          ZIO.logDebug(s"Updated entity state for ${entityState.entityId} -> $result")
      case _ => ZIO.unit
    }

  override val supportedType: Type = Type.SubscribeEvents

}

object SubscribeEntitiesHandler {

  val layer: URLayer[RequestRepository & EntityStateRepository, SubscribeEntitiesHandler] = ZLayer {
    for {
      requestRepository <- ZIO.service[RequestRepository]
      entityStateRepository <- ZIO.service[EntityStateRepository]
    } yield SubscribeEntitiesHandler(entityStateRepository, requestRepository)
  }

}
