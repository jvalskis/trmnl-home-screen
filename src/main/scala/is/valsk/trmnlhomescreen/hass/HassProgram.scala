package is.valsk.trmnlhomescreen.hass

import is.valsk.trmnlhomescreen.hass.HassConfig
import is.valsk.trmnlhomescreen.hass.protocol.{ChannelHandler, ProtocolHandler, TextHandler, UnhandledMessageHandler}
import is.valsk.trmnlhomescreen.hass.protocol.ChannelHandler.PartialChannelHandler
import is.valsk.trmnlhomescreen.hass.protocol.api.{AuthenticationHandler, ConnectHandler, HassResponseMessageHandler, ResultHandler}
import zio.*
import zio.http.*

trait HassProgram {
  def run: Task[Nothing]
}

object HassProgram {

  private class HassProgramLive(
      channelHandler: PartialChannelHandler,
      config: HassConfig,
  ) extends HassProgram {

    def run: Task[Nothing] = {
      val client = Handler
        .webSocket { channel =>
          channel.receiveAll(event => channelHandler(channel, event))
        }
        .connect(config.webSocketUrl)
      for {
        _ <- ZIO.logInfo(s"Connecting to HASS @ ${config.webSocketUrl}")
        result <- (client *> ZIO.never)
          .provide(
            Client.default,
            Scope.default,
          )
          .logError("Error connecting to HASS")
      } yield result
    }

  }

  val layer: URLayer[HassConfig & List[ChannelHandler], HassProgram] = ZLayer {
    for {
      config <- ZIO.service[HassConfig]
      channelHandlers <- ZIO.service[List[ChannelHandler]]
      combinedHandler = channelHandlers.foldLeft(ChannelHandler.empty) { (a, b) => a orElse b.get }
    } yield HassProgramLive(combinedHandler, config)
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

  val configuredLayer: ULayer[HassProgram] =
    ResultHandler.layer >>> ProtocolHandler.layer >>> TextHandler.layer >>> UnhandledMessageHandler.layer >>> HassConfig.layer >>> hassResponseMessageHandlerLayer >>> channelHandlerLayer >>> layer

}
