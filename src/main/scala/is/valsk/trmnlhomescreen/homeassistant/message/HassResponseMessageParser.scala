package is.valsk.trmnlhomescreen.homeassistant.message

import is.valsk.trmnlhomescreen.homeassistant.message.HassResponseMessageParser.TypedMessage
import is.valsk.trmnlhomescreen.homeassistant.message.MessageParser.ParseError
import is.valsk.trmnlhomescreen.homeassistant.message.model.HassResponseMessage
import is.valsk.trmnlhomescreen.homeassistant.message.model.responses.{AuthInvalid, AuthOK, AuthRequired, Event, Result}
import zio.*
import zio.json.*

class HassResponseMessageParser extends MessageParser[HassResponseMessage] {

  def parseMessage(json: String): IO[ParseError, HassResponseMessage] = {
    json.fromJson[TypedMessage] match {
      case Left(value) =>
        ZIO.fail(ParseError(value))
      case Right(TypedMessage(messageType)) =>
        val parseResult = messageType match {
          case Type.AuthRequired => json.fromJson[AuthRequired]
          case Type.AuthOK => json.fromJson[AuthOK]
          case Type.AuthInvalid => json.fromJson[AuthInvalid]
          case Type.Result => json.fromJson[Result]
          case Type.Event => json.fromJson[Event]
          case other => Left(s"Unsupported type: $other")
        }
        parseResult match
          case Left(value) => ZIO.fail(ParseError(value))
          case Right(value) => ZIO.succeed(value)
      case _ => ZIO.fail(ParseError("Failed to extract HASS message type"))
    }
  }

}

object HassResponseMessageParser {

  val layer: ULayer[MessageParser[HassResponseMessage]] = ZLayer.succeed(HassResponseMessageParser())

  case class TypedMessage(
      `type`: String,
  )

  object TypedMessage {
    given decoder: JsonDecoder[TypedMessage] = DeriveJsonDecoder.gen[TypedMessage]

    def unapply(typedMessage: TypedMessage): Option[Type] = Type.parse(typedMessage.`type`).toOption
  }

}
