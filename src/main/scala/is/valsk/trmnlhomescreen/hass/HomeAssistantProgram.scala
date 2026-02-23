package is.valsk.trmnlhomescreen.hass

import is.valsk.trmnlhomescreen.Program
import is.valsk.trmnlhomescreen.hass.messages.{HassResponseMessageParser, SequentialMessageIdGenerator}
import is.valsk.trmnlhomescreen.hass.protocol.ChannelHandler.PartialChannelHandler
import is.valsk.trmnlhomescreen.hass.protocol.api.{AuthenticationHandler, ConnectHandler, HassResponseMessageHandler, MessageSender, ResultHandler}
import is.valsk.trmnlhomescreen.hass.protocol.{ChannelHandler, ProtocolHandler, TextHandler, UnhandledMessageHandler}
import zio.http.{Client, Handler}
import zio.{RLayer, Scope, Task, URLayer, ZIO, ZLayer}

trait HomeAssistantProgram extends Program

object HomeAssistantProgram {

  private class HomeAssistantProgramLive(
      channelHandler: PartialChannelHandler,
      config: HomeAssistantConfig,
//      client: HomeAssistantClient,
      renderer: HomeAssistantRenderer,
  ) extends HomeAssistantProgram {

    def run: Task[Unit] =
      runIfEnabled(config.enabled, "Home Assistant feature is disabled") {
        val client = Handler
          .webSocket { channel =>
            channel.receiveAll(event => channelHandler(channel, event))
          }
          .connect(config.webSocketUrl)
        for {
          _ <- ZIO.logInfo(s"Connecting to HASS @ ${config.webSocketUrl}")
          _ <- (client *> ZIO.never)
            .provide(
              Client.default,
              Scope.default,
            )
            .logError("Error connecting to HASS")
        } yield ()
//        for
//          _ <- ZIO.logInfo(s"Starting Home Assistant integration for ${config.host}:${config.port}")
//          interval = Duration.fromSeconds(config.fetchIntervalMinutes.toLong * 60)
//          _ <- fetchAndPrint
//            .catchAll(e => ZIO.logError(s"Failed to fetch Home Assistant data: ${e.getMessage}"))
//            .repeat(Schedule.fixed(interval))
//        yield ()
      }

//    private def fetchAndPrint: Task[Unit] =
//      for
//        areaMapping <- client.fetchAreaRegistry()
//        states <- client.fetchEntityStates()
//        areaGroups = groupByArea(states, areaMapping)
//        rendered <- renderer.render(areaGroups)
//        _ <- Console.printLine(rendered)
//      yield ()
//
//    private def groupByArea(
//        states: List[EntityState],
//        areaMapping: Map[String, String],
//    ): List[AreaGroup] =
//      val entitiesWithArea = states.map { state =>
//        EntityWithArea(
//          entityId = state.entityId,
//          friendlyName = state.attributes.friendlyName.getOrElse(state.entityId),
//          state = state.state,
//          unitOfMeasurement = state.attributes.unitOfMeasurement.getOrElse(""),
//          areaName = areaMapping.getOrElse(state.entityId, "Unknown"),
//        )
//      }
//      entitiesWithArea
//        .groupBy(_.areaName)
//        .toList
//        .sortBy(_._1)
//        .map { case (areaName, entities) => AreaGroup(areaName, entities) }

  }

  val layer: URLayer[
    HomeAssistantRenderer & HomeAssistantConfig & List[ChannelHandler],
    HomeAssistantProgram,
  ] =
    ZLayer {
      for {
        config <- ZIO.service[HomeAssistantConfig]
//        client <- ZIO.service[HomeAssistantClient]
        renderer <- ZIO.service[HomeAssistantRenderer]
        channelHandlers <- ZIO.service[List[ChannelHandler]]
        combinedHandler = channelHandlers.foldLeft(ChannelHandler.empty) { (a, b) => a orElse b.get }
      } yield HomeAssistantProgramLive(combinedHandler, config, renderer)
    }

  private val channelHandlerLayer
      : URLayer[ProtocolHandler & TextHandler & UnhandledMessageHandler, List[ChannelHandler]] = ZLayer {
    for {
      protocolHandler <- ZIO.service[ProtocolHandler]
      textHandler <- ZIO.service[TextHandler]
      unhandledMessageHandler <- ZIO.service[UnhandledMessageHandler]
    } yield List(protocolHandler, textHandler, unhandledMessageHandler)
  }

  private val hassResponseMessageHandlerLayer
      : URLayer[AuthenticationHandler & ConnectHandler & ResultHandler, List[HassResponseMessageHandler]] = ZLayer {
    for {
      authenticationHandler <- ZIO.service[AuthenticationHandler]
      connectHandler <- ZIO.service[ConnectHandler]
      resultHandler <- ZIO.service[ResultHandler]
    } yield List(authenticationHandler, connectHandler, resultHandler)
  }

  val configuredLayer: RLayer[HomeAssistantRenderer, HomeAssistantProgram] =
    ZLayer.makeSome[HomeAssistantRenderer, HomeAssistantProgram](
      layer,
      channelHandlerLayer,
      hassResponseMessageHandlerLayer,
      TextHandler.layer,
      ProtocolHandler.layer,
      UnhandledMessageHandler.layer,
      AuthenticationHandler.layer,
      ConnectHandler.layer,
      MessageSender.layer,
      ResultHandler.layer,
      HassResponseMessageParser.layer,
      SequentialMessageIdGenerator.layer,
      HomeAssistantConfig.layer,
    )

}
