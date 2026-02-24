package is.valsk.trmnlhomescreen.homeassistant.protocol.handlers

import is.valsk.trmnlhomescreen.ScreenStateRepository
import is.valsk.trmnlhomescreen.homeassistant.HomeAssistantConfig
import is.valsk.trmnlhomescreen.homeassistant.message.model.responses.Event.EntityState
import is.valsk.trmnlhomescreen.homeassistant.message.model.responses.Result
import is.valsk.trmnlhomescreen.homeassistant.message.{RequestRepository, Type}
import zio.*
import zio.json.*

object GetStatesHandler {

  val layer: URLayer[RequestRepository & ScreenStateRepository & HomeAssistantConfig, HomeAssistantResultHandler] = {
    ZLayer {
      for {
        screenStateRepository <- ZIO.service[ScreenStateRepository]
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
            screenStateRepository.updateEntityState(state.entityId, state) *>
              ZIO.logDebug(s"Updated entity state for ${state.entityId} -> $state"),
          )
        },
        supportedType = Type.GetStates,
      )

    }

  }

  val configuredLayer: RLayer[ScreenStateRepository & RequestRepository, HomeAssistantResultHandler] =
    HomeAssistantConfig.layer >>> layer

}
