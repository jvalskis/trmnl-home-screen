package is.valsk.trmnlhomescreen.hass.protocol.handlers

import is.valsk.trmnlhomescreen.hass.{EntityState, HomeAssistantConfig}
import is.valsk.trmnlhomescreen.hass.messages.responses.*
import is.valsk.trmnlhomescreen.hass.messages.{HassIdentifiableMessage, HassResponseMessage, Type}
import is.valsk.trmnlhomescreen.hass.protocol.api.{EntityStateRepository, RequestRepository}
import zio.*
import zio.http.WebSocketChannel
import zio.json.*

class GetStatesHandler(
    entityStateRepository: EntityStateRepository,
    val requestRepository: RequestRepository,
    allowedEntityIds: Set[String],
) extends HomeAssistantResultHandler {

  override def handleInternal(channel: WebSocketChannel, result: HassResponseMessage & HassIdentifiableMessage) =
    result match {
      case result: Result =>
        ZIO.foreachDiscard(
          result.result.toSeq
            .flatMap(_.toJson.fromJson[List[EntityState]].toOption)
            .flatten
            .filter(state => allowedEntityIds.contains(state.entityId)),
        )(state =>
          entityStateRepository.add(state.entityId, state) *>
            ZIO.logDebug(s"Updated entity state for ${state.entityId} -> $state"),
        )
      case _ => ZIO.unit
    }

  override protected val supportedType: Type = Type.GetStates
}

object GetStatesHandler {

  val layer: URLayer[RequestRepository & EntityStateRepository & HomeAssistantConfig, GetStatesHandler] = ZLayer {
    for {
      requestRepository <- ZIO.service[RequestRepository]
      entityStateRepository <- ZIO.service[EntityStateRepository]
      config <- ZIO.service[HomeAssistantConfig]
    } yield GetStatesHandler(entityStateRepository, requestRepository, config.subscribedEntityIdList.toSet)
  }

}
