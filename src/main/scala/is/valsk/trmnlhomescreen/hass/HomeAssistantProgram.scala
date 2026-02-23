package is.valsk.trmnlhomescreen.hass

import is.valsk.trmnlhomescreen.Program
import is.valsk.trmnlhomescreen.hass.messages.{HassResponseMessageParser, SequentialMessageIdGenerator, Type}
import is.valsk.trmnlhomescreen.hass.protocol.*
import is.valsk.trmnlhomescreen.hass.protocol.ChannelHandler.PartialChannelHandler
import is.valsk.trmnlhomescreen.hass.protocol.api.RequestRepository
import is.valsk.trmnlhomescreen.hass.protocol.handlers.*
import zio.http.{Client, Handler, SocketDecoder, WebSocketConfig}
import zio.{Duration, RLayer, Schedule, Scope, Task, URLayer, ZIO, ZLayer}

trait HomeAssistantProgram extends Program

object HomeAssistantProgram {

  private class HomeAssistantProgramLive(
      channelHandler: PartialChannelHandler,
      config: HomeAssistantConfig,
      renderer: HomeAssistantRenderer,
  ) extends HomeAssistantProgram {

    def run: Task[Unit] =
      runIfEnabled(config.enabled, "Home Assistant feature is disabled") {
        val wsConfig = WebSocketConfig.default.decoderConfig(
          SocketDecoder.default.maxFramePayloadLength(10 * 1024 * 1024),
        )
        val client = Handler
          .webSocket { channel =>
            channel.receiveAll(event => channelHandler(channel, event))
          }
          .withConfig(wsConfig)
          .connect(config.webSocketUrl)
        val retrySchedule = Schedule.exponential(Duration.fromSeconds(1), 2.0) &&
          Schedule.recurs(10) &&
          Schedule.recurWhile[Throwable](_ => true)
        val connect = for {
          _ <- ZIO.logInfo(s"Connecting to HASS @ ${config.webSocketUrl}")
          _ <- client
            .provide(
              Client.default,
              Scope.default,
            )
          _ <- ZIO.fail(new RuntimeException("Connection closed"))
        } yield ()
        connect
          .tapError(e => ZIO.logError(s"Connection to HASS lost: ${e.getMessage}"))
          .retry(retrySchedule.tapOutput((duration, _, _) =>
            ZIO.logInfo(s"Reconnecting in ${duration.toSeconds}s...")
          ))
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

  private val hassResponseMessageHandlerLayer: URLayer[
    AuthenticationHandler & ResultHandler,
    List[HassResponseMessageHandler],
  ] = ZLayer {
    for {
      authenticationHandler <- ZIO.service[AuthenticationHandler]
      resultHandler <- ZIO.service[ResultHandler]
    } yield List(authenticationHandler, resultHandler)
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
      MessageSender.layer,
      ResultHandler.layer,
      RequestRepository.layer,
      ZLayer.succeed(Map.empty[Type, HomeAssistantResultHandler]),
      CommandPhaseHandlerLive.layer,
      HassResponseMessageParser.layer,
      SequentialMessageIdGenerator.layer,
      HomeAssistantConfig.layer,
    )

}
