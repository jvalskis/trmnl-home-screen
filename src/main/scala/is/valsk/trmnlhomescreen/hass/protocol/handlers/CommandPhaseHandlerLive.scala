package is.valsk.trmnlhomescreen.hass.protocol.handlers

import is.valsk.trmnlhomescreen.hass.messages.commands.{GetStatesCommand, SubscribeEntitiesCommand}
import is.valsk.trmnlhomescreen.hass.protocol.MessageSender
import is.valsk.trmnlhomescreen.hass.protocol.handlers.AuthenticationHandler.CommandPhaseHandler
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
          _ <- messageSender.send(SubscribeEntitiesCommand(Seq("sensor.augustas_humidity", "svetaine_dregmes_ir_temperaturos_sensorius_temperature"))) *> ZIO.logInfo("Subscribing to entity events")
          _ <- messageSender.send(GetStatesCommand()) *> ZIO.logInfo("Getting entity states")
        } yield ()
    }
  }

}
