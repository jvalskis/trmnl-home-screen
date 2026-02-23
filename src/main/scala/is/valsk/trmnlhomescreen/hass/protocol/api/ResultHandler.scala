package is.valsk.trmnlhomescreen.hass.protocol.api

import is.valsk.trmnlhomescreen.hass.messages.MessageIdGenerator
import is.valsk.trmnlhomescreen.hass.messages.MessageParser.ParseError
import is.valsk.trmnlhomescreen.hass.messages.responses.*
import is.valsk.trmnlhomescreen.hass.protocol.api.HassResponseMessageHandler.{HassResponseMessageContext, PartialHassResponseMessageHandler}
import zio.*
import zio.http.*
import zio.json.*

class ResultHandler(
    idGen: MessageIdGenerator,
    requestMap: Ref[Map[Int, String]],
) extends HassResponseMessageHandler {

  override def get: PartialHassResponseMessageHandler = { case HassResponseMessageContext(_, result: Result) =>
    ZIO.foreachDiscard(result.result.map(_.toJson))(response =>
//      result.catchAll { case error =>
//        ZIO.logError(s"Failed to handle device. Error: ${error.getMessage}. HASS Device: $hassDevice")
//      },
      ZIO.logInfo(s"Response: $response"),
    )
  }

}

object ResultHandler {

  val layer: URLayer[MessageIdGenerator, ResultHandler] = ZLayer {
    for {
      generator <- ZIO.service[MessageIdGenerator]
      ref <- Ref.make[Map[Int, String]](Map.empty)
    } yield ResultHandler(generator, ref)
  }

}
