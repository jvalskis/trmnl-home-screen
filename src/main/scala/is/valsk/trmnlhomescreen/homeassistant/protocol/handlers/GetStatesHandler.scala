package is.valsk.trmnlhomescreen.homeassistant.protocol.handlers

import is.valsk.trmnlhomescreen.homeassistant.HomeAssistantStateRepository
import is.valsk.trmnlhomescreen.homeassistant.HomeAssistantConfig
import is.valsk.trmnlhomescreen.homeassistant.message.model.responses.Event.EntityState
import is.valsk.trmnlhomescreen.homeassistant.message.model.responses.Result
import is.valsk.trmnlhomescreen.homeassistant.message.{RequestRepository, Type}
import zio.*
import zio.json.*

object GetStatesHandler {

  val layer: URLayer[RequestRepository & HomeAssistantStateRepository & HomeAssistantConfig, HomeAssistantResultHandler] = {
    ZLayer {
      for {
        homeAssistantStateRepository <- ZIO.service[HomeAssistantStateRepository]
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
            homeAssistantStateRepository.updateEntityState(state.entityId, state) *>
              ZIO.logDebug(s"Updated entity state for ${state.entityId} -> $state"),
          )
        },
        supportedType = Type.GetStates,
      )

    }

  }

  val configuredLayer: RLayer[HomeAssistantStateRepository & RequestRepository, HomeAssistantResultHandler] =
    HomeAssistantConfig.layer >>> layer

}
