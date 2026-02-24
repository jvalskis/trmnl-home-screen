package is.valsk.trmnlhomescreen.hass.protocol.handlers

import is.valsk.trmnlhomescreen.hass.messages.responses.*
import is.valsk.trmnlhomescreen.hass.messages.{HassIdentifiableMessage, HassResponseMessage, Type}
import is.valsk.trmnlhomescreen.hass.protocol.api.{EntityStateRepository, RequestRepository}
import is.valsk.trmnlhomescreen.hass.{EntityState, HomeAssistantConfig}
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
