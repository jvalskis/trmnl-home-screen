package is.valsk.trmnlhomescreen.hass.protocol.api

import is.valsk.trmnlhomescreen.hass.HomeAssistantConfig
import is.valsk.trmnlhomescreen.hass.messages.commands.Auth
import is.valsk.trmnlhomescreen.hass.messages.responses.{AuthInvalid, AuthRequired}
import is.valsk.trmnlhomescreen.hass.protocol.api.HassResponseMessageHandler.{HassResponseMessageContext, PartialHassResponseMessageHandler}
import zio.*
import zio.http.*
import zio.http.ChannelEvent.Read
import zio.json.*

class AuthenticationHandler(config: HomeAssistantConfig) extends HassResponseMessageHandler {

  override def get: PartialHassResponseMessageHandler = {
    case HassResponseMessageContext(channel, message: AuthInvalid) => handleAuthInvalid(channel, message)
    case HassResponseMessageContext(channel, _: AuthRequired) => handleAuthRequired(channel)
  }

  private def handleAuthInvalid(channel: WebSocketChannel, message: AuthInvalid): Task[Unit] = for {
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
}

object AuthenticationHandler {
  val layer: URLayer[HomeAssistantConfig, AuthenticationHandler] = ZLayer {
    for {
      config <- ZIO.service[HomeAssistantConfig]
    } yield AuthenticationHandler(config)
  }

  val configuredLayer: TaskLayer[AuthenticationHandler] = HomeAssistantConfig.layer >>> layer
}
