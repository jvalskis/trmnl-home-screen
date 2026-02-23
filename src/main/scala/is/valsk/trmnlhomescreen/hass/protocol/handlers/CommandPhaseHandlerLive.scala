package is.valsk.trmnlhomescreen.hass.protocol.handlers

import is.valsk.trmnlhomescreen.hass.HomeAssistantConfig
import is.valsk.trmnlhomescreen.hass.messages.commands.{GetStatesCommand, SubscribeEventsCommand}
import is.valsk.trmnlhomescreen.hass.protocol.MessageSender
import is.valsk.trmnlhomescreen.hass.protocol.handlers.AuthenticationHandler.CommandPhaseHandler
import zio.{Task, URLayer, ZIO, ZLayer}
import zio.http.WebSocketChannel

object CommandPhaseHandlerLive {

  val layer: URLayer[MessageSender & HomeAssistantConfig, CommandPhaseHandler] = ZLayer {
    for {
      messageSender <- ZIO.service[MessageSender]
      config <- ZIO.service[HomeAssistantConfig]
    } yield new CommandPhaseHandler {
      def apply(using channel: WebSocketChannel): Task[Unit] =
        for {
          _ <- ZIO.logInfo("Beginning command phase")
          _ <- messageSender.send(SubscribeEventsCommand(config.subscribedEntityIdList)) *> ZIO.logInfo("Subscribing to entity events")
          _ <- messageSender.send(GetStatesCommand()) *> ZIO.logInfo("Getting entity states")
        } yield ()
    }
  }

}
