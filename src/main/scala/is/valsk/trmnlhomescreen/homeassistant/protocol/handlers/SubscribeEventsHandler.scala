package is.valsk.trmnlhomescreen.homeassistant.protocol.handlers

import is.valsk.trmnlhomescreen.homeassistant.message.model.responses.Event.EntityState
import is.valsk.trmnlhomescreen.homeassistant.message.model.responses.Event
import is.valsk.trmnlhomescreen.homeassistant.message.{RequestRepository, Type}
import is.valsk.trmnlhomescreen.homeassistant.{EntityStateRepository, HomeAssistantConfig}
import zio.*
import zio.http.WebSocketChannel

object SubscribeEventsHandler {

  val layer: URLayer[RequestRepository & EntityStateRepository, HomeAssistantResultHandler] =
    ZLayer {
      for {
        entityStateRepository <- ZIO.service[EntityStateRepository]
        requestRepository <- ZIO.service[RequestRepository]
      } yield new HomeAssistantResultHandler(
        requestRepository = requestRepository,
        handler = { case (_, event: Event, _) =>
          val entityState = event.event.data.newState
          entityStateRepository.add(entityState.entityId, entityState) *>
            ZIO.logDebug(s"Updated entity state for ${entityState.entityId} -> $event")
        },
        supportedType = Type.SubscribeEvents,
      )

    }

  val configuredLayer: RLayer[EntityStateRepository & RequestRepository, HomeAssistantResultHandler] =
    HomeAssistantConfig.layer >>> layer

}
