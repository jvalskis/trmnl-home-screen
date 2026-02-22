package is.valsk.trmnlhomescreen.hass.protocol.api

import is.valsk.trmnlhomescreen.hass.messages.MessageParser.ParseError
import is.valsk.trmnlhomescreen.hass.messages.responses.*
import is.valsk.trmnlhomescreen.hass.protocol.api.HassResponseMessageHandler.{HassResponseMessageContext, PartialHassResponseMessageHandler}
import zio.*
import zio.http.*

class ResultHandler(
) extends HassResponseMessageHandler {

  override def get: PartialHassResponseMessageHandler = {
    case HassResponseMessageContext(_, result: Result) =>
      ZIO.foreachDiscard(result.result.toSeq.flatten)(hassDevice =>
        result.catchAll {
          case error =>
            ZIO.logError(s"Failed to handle device. Error: ${error.getMessage}. HASS Device: $hassDevice")
        }
      )
  }

}

object ResultHandler {
  val layer: ULayer[ResultHandler] = ZLayer.fromFunction(ResultHandler(_, _))
}