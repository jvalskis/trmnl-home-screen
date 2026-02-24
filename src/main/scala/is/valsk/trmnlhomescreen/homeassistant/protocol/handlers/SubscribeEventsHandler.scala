package is.valsk.trmnlhomescreen.homeassistant.protocol.handlers

import is.valsk.trmnlhomescreen.ScreenStateRepository
import is.valsk.trmnlhomescreen.homeassistant.message.model.responses.Event
import is.valsk.trmnlhomescreen.homeassistant.message.{RequestRepository, Type}
import is.valsk.trmnlhomescreen.homeassistant.HomeAssistantConfig
import zio.*

object SubscribeEventsHandler {

  val layer: URLayer[RequestRepository & ScreenStateRepository, HomeAssistantResultHandler] =
    ZLayer {
      for {
        screenStateRepository <- ZIO.service[ScreenStateRepository]
        requestRepository <- ZIO.service[RequestRepository]
      } yield new HomeAssistantResultHandler(
        requestRepository = requestRepository,
        handler = { case (_, event: Event, _) =>
          val entityState = event.event.data.newState
          screenStateRepository.updateEntityState(entityState.entityId, entityState) *>
            ZIO.logDebug(s"Updated entity state for ${entityState.entityId} -> $event")
        },
        supportedType = Type.SubscribeEvents,
      )

    }

  val configuredLayer: RLayer[ScreenStateRepository & RequestRepository, HomeAssistantResultHandler] =
    HomeAssistantConfig.layer >>> layer

}
