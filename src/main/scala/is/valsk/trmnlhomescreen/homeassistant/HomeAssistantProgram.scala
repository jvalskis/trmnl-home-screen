package is.valsk.trmnlhomescreen.homeassistant

import is.valsk.trmnlhomescreen.{Program, ScreenStateRepository}
import is.valsk.trmnlhomescreen.homeassistant.message.MessageIdGenerator.SequentialMessageIdGenerator
import is.valsk.trmnlhomescreen.homeassistant.message.{HassResponseMessageParser, MessageSender, RequestRepository}
import is.valsk.trmnlhomescreen.homeassistant.protocol.*
import is.valsk.trmnlhomescreen.homeassistant.protocol.ChannelHandler.PartialChannelHandler
import is.valsk.trmnlhomescreen.homeassistant.protocol.handlers.*
import zio.http.{Client, Handler, SocketDecoder, WebSocketConfig}
import zio.{Duration, RLayer, Schedule, Scope, Task, URLayer, ZIO, ZLayer}

trait HomeAssistantProgram extends Program

object HomeAssistantProgram {

  private class HomeAssistantProgramLive(
      channelHandler: PartialChannelHandler,
      config: HomeAssistantConfig,
  ) extends HomeAssistantProgram {

    def run: Task[Unit] =
      runIfEnabled(config.enabled, "Home Assistant feature is disabled") {
        val wsConfig = WebSocketConfig.default.decoderConfig(
          SocketDecoder.default.maxFramePayloadLength(config.maxFrameSizeKb * 1024),
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
          .retry(
            retrySchedule.tapOutput((duration, _, _) => ZIO.logInfo(s"Reconnecting in ${duration.toSeconds}s...")),
          )
      }

  }

  val layer: URLayer[
    HomeAssistantConfig & List[ChannelHandler],
    HomeAssistantProgram,
  ] =
    ZLayer {
      for {
        config <- ZIO.service[HomeAssistantConfig]
        channelHandlers <- ZIO.service[List[ChannelHandler]]
        combinedHandler = channelHandlers.foldLeft(ChannelHandler.empty) { (a, b) => a orElse b.get }
      } yield HomeAssistantProgramLive(combinedHandler, config)
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

  val configuredLayer: RLayer[ScreenStateRepository, HomeAssistantProgram] =
    ZLayer.makeSome[ScreenStateRepository, HomeAssistantProgram](
      layer,
      channelHandlerLayer,
      hassResponseMessageHandlerLayer,
      TextHandler.layer,
      ProtocolHandler.layer,
      UnhandledMessageHandler.layer,
      AuthenticationHandler.configuredLayer,
      MessageSender.layer,
      ResultHandler.layer,
      RequestRepository.layer,
      ZLayer.scoped {
        for {
          getStates <- GetStatesHandler.layer.build.map(_.get)
          subscribeEvents <- SubscribeEventsHandler.layer.build.map(_.get)
        } yield Seq(getStates, subscribeEvents)
      },
      CommandPhaseHandlerLive.layer,
      HassResponseMessageParser.layer,
      SequentialMessageIdGenerator.layer,
      HomeAssistantConfig.layer,
    )

}
