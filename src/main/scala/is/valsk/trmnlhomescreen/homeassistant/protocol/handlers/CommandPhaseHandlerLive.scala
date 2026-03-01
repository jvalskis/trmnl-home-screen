package is.valsk.trmnlhomescreen.homeassistant.protocol.handlers

import is.valsk.trmnlhomescreen.homeassistant.message.MessageSender
import is.valsk.trmnlhomescreen.homeassistant.message.model.commands.{GetAreaRegistryCommand, GetDeviceRegistryCommand, GetEntityRegistryCommand, GetStatesCommand, SubscribeEventsCommand}
import is.valsk.trmnlhomescreen.homeassistant.protocol.handlers.AuthenticationHandler.CommandPhaseHandler
import zio.{Task, URLayer, ZIO, ZLayer}
import zio.http.WebSocketChannel

object CommandPhaseHandlerLive {

  val layer: URLayer[MessageSender, CommandPhaseHandler] = ZLayer {
    for {
      messageSender <- ZIO.service[MessageSender]
    } yield new CommandPhaseHandler {
      def apply(using channel: WebSocketChannel): Task[Unit] =
        for {
          _ <- ZIO.logInfo("Beginning command phase")
          _ <- messageSender.send(SubscribeEventsCommand()) *> ZIO.logInfo("Subscribing to entity events")
          _ <- messageSender.send(GetStatesCommand()) *> ZIO.logInfo("Getting entity states")
          _ <- messageSender.send(GetAreaRegistryCommand()) *> ZIO.logInfo("Getting area registry")
          _ <- messageSender.send(GetEntityRegistryCommand()) *> ZIO.logInfo("Getting entity registry")
          _ <- messageSender.send(GetDeviceRegistryCommand()) *> ZIO.logInfo("Getting device registry")
        } yield ()
    }
  }

}
