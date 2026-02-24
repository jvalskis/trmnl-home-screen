package is.valsk.trmnlhomescreen.hass.protocol.handlers

import is.valsk.trmnlhomescreen.hass.HomeAssistantConfig
import is.valsk.trmnlhomescreen.hass.messages.commands.Auth
import is.valsk.trmnlhomescreen.hass.messages.responses.{AuthInvalid, AuthOK, AuthRequired}
import is.valsk.trmnlhomescreen.hass.protocol.handlers.AuthenticationHandler.CommandPhaseHandler
import is.valsk.trmnlhomescreen.hass.protocol.handlers.HassResponseMessageHandler.{HassResponseMessageContext, PartialHassResponseMessageHandler}
import zio.*
import zio.http.*
import zio.http.ChannelEvent.Read
import zio.json.*

class AuthenticationHandler(config: HomeAssistantConfig, commandPhaseHandler: CommandPhaseHandler)
    extends HassResponseMessageHandler {

  override def get: PartialHassResponseMessageHandler = {
    case HassResponseMessageContext(channel, message: AuthInvalid) => handleAuthInvalid(channel, message)
    case HassResponseMessageContext(channel, _: AuthRequired) => handleAuthRequired(channel)
    case HassResponseMessageContext(channel, _: AuthOK) => handleAuthOK(channel)
  }

  private def handleAuthInvalid(channel: WebSocketChannel, message: AuthInvalid): Task[Unit] =
    for {
      _ <- ZIO.logInfo(s"AuthInvalid: ${message.message}")
      _ <- channel.awaitShutdown
    } yield ()

  private def handleAuthRequired(channel: WebSocketChannel) = {
    for {
      _ <- ZIO.logInfo(s"AuthRequired: sending auth message")
      authMessage = Auth(config.accessToken)
      _ <- channel.send(Read(WebSocketFrame.text(authMessage.toJson)))
    } yield ()
  }

  private def handleAuthOK(channel: WebSocketChannel) = {
    for {
      _ <- ZIO.logInfo(s"AuthOK. Beginning command phase")
      _ <- commandPhaseHandler(using channel)
    } yield ()
  }

}

object AuthenticationHandler {

  val layer: URLayer[HomeAssistantConfig & CommandPhaseHandler, AuthenticationHandler] = ZLayer {
    for {
      config <- ZIO.service[HomeAssistantConfig]
      commandPhaseHandler <- ZIO.service[CommandPhaseHandler]
    } yield AuthenticationHandler(config, commandPhaseHandler)
  }

  val configuredLayer: RLayer[CommandPhaseHandler, AuthenticationHandler] = HomeAssistantConfig.layer >>> layer

  trait CommandPhaseHandler {
    def apply(using channel: WebSocketChannel): Task[Unit]
  }

}
