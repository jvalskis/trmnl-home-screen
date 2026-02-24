package is.valsk.trmnlhomescreen.homeassistant.protocol.handlers

import is.valsk.trmnlhomescreen.homeassistant.message.model.responses.Event.EntityState
import is.valsk.trmnlhomescreen.homeassistant.message.model.responses.Result
import is.valsk.trmnlhomescreen.homeassistant.message.{RequestRepository, Type}
import is.valsk.trmnlhomescreen.homeassistant.{EntityStateRepository, HomeAssistantConfig}
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
