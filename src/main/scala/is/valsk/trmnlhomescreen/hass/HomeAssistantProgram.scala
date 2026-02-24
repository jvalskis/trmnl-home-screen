package is.valsk.trmnlhomescreen.hass

import is.valsk.trmnlhomescreen.Program
import is.valsk.trmnlhomescreen.hass.messages.{HassResponseMessageParser, SequentialMessageIdGenerator, Type}
import is.valsk.trmnlhomescreen.hass.protocol.*
import is.valsk.trmnlhomescreen.hass.protocol.ChannelHandler.PartialChannelHandler
import is.valsk.trmnlhomescreen.hass.protocol.api.{EntityStateRepository, RequestRepository}
import is.valsk.trmnlhomescreen.hass.protocol.handlers.*
import zio.http.{Client, Handler, SocketDecoder, WebSocketConfig}
import zio.{Duration, RLayer, Schedule, Scope, Task, URLayer, ZIO, ZLayer}

trait HomeAssistantProgram extends Program

object HomeAssistantProgram {

  private class HomeAssistantProgramLive(
      channelHandler: PartialChannelHandler,
      config: HomeAssistantConfig,
      renderer: HomeAssistantRenderer,
      entityStateRepository: EntityStateRepository,
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
        val renderLoop = (renderEntities *> ZIO.sleep(Duration.fromSeconds(30))).forever

        connect
          .tapError(e => ZIO.logError(s"Connection to HASS lost: ${e.getMessage}"))
          .retry(
            retrySchedule.tapOutput((duration, _, _) => ZIO.logInfo(s"Reconnecting in ${duration.toSeconds}s...")),
          ) <&> renderLoop
      }

    private def renderEntities: Task[Unit] =
      for {
        entities <- entityStateRepository.getAll
        rendered <- renderer.render(entities)
        _ <- ZIO.logInfo(s"\n$rendered")
      } yield ()

  }

  val layer: URLayer[
    HomeAssistantRenderer & HomeAssistantConfig & List[ChannelHandler] & EntityStateRepository,
    HomeAssistantProgram,
  ] =
    ZLayer {
      for {
        config <- ZIO.service[HomeAssistantConfig]
        renderer <- ZIO.service[HomeAssistantRenderer]
        channelHandlers <- ZIO.service[List[ChannelHandler]]
        entityStateRepository <- ZIO.service[EntityStateRepository]
        combinedHandler = channelHandlers.foldLeft(ChannelHandler.empty) { (a, b) => a orElse b.get }
      } yield HomeAssistantProgramLive(combinedHandler, config, renderer, entityStateRepository)
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
      AuthenticationHandler.configuredLayer,
      MessageSender.layer,
      ResultHandler.layer,
      RequestRepository.layer,
      EntityStateRepository.layer,
      ZLayer {
        for {
          getStatesHandler <- ZIO.service[GetStatesHandler]
          subscribeEventsHandler <- ZIO.service[SubscribeEventsHandler]
        } yield Map[Type, HomeAssistantResultHandler](
          Type.GetStates -> getStatesHandler,
          Type.SubscribeEvents -> subscribeEventsHandler,
        )
      },
      GetStatesHandler.layer,
      SubscribeEventsHandler.layer,
      CommandPhaseHandlerLive.layer,
      HassResponseMessageParser.layer,
      SequentialMessageIdGenerator.layer,
      HomeAssistantConfig.layer,
    )

}
