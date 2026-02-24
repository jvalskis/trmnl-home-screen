package is.valsk.trmnlhomescreen.hass.protocol.handlers

import is.valsk.trmnlhomescreen.hass.messages.Type
import is.valsk.trmnlhomescreen.hass.messages.responses.*
import is.valsk.trmnlhomescreen.hass.protocol.api.{EntityStateRepository, RequestRepository}
import is.valsk.trmnlhomescreen.hass.{EntityState, HomeAssistantConfig}
import zio.*
import zio.json.*

object GetStatesHandler {

  val layer: URLayer[RequestRepository & EntityStateRepository & HomeAssistantConfig, HomeAssistantResultHandler] = {
    ZLayer {
      for {
        entityStateRepository <- ZIO.service[EntityStateRepository]
        requestRepository <- ZIO.service[RequestRepository]
        config <- ZIO.service[HomeAssistantConfig]
        allowedEntityIds = config.subscribedEntityIdList.toSet
      } yield new HomeAssistantResultHandler(
        requestRepository = requestRepository,
        handler = { case (_, result: Result, _) =>
          ZIO.foreachDiscard(
            result.result.toSeq
              .flatMap(_.toJson.fromJson[List[EntityState]].toOption)
              .flatten
              .filter(state => allowedEntityIds.contains(state.entityId)),
          )(state =>
            entityStateRepository.add(state.entityId, state) *>
              ZIO.logDebug(s"Updated entity state for ${state.entityId} -> $state"),
          )
        },
        supportedType = Type.GetStates,
      )

    }

  }

  val configuredLayer: RLayer[EntityStateRepository & RequestRepository, HomeAssistantResultHandler] =
    HomeAssistantConfig.layer >>> layer

}
