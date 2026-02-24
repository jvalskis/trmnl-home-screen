package is.valsk.trmnlhomescreen.homeassistant.message

import is.valsk.trmnlhomescreen.homeassistant.message.MessageParser.ParseError
import zio.json.{DeriveJsonDecoder, DeriveJsonEncoder, JsonDecoder, JsonEncoder}

enum Type(val typeName: String) {
  case Auth extends Type("auth")

  case AuthRequired extends Type("auth_required")
  case AuthOK extends Type("auth_ok")
  case AuthInvalid extends Type("auth_invalid")
  case Result extends Type("result")
  case Event extends Type("event")
  case SubscribeEvents extends Type("subscribe_events")
  case GetStates extends Type("get_states")
}

object Type {
  given typeDecoder: JsonDecoder[Type] = DeriveJsonDecoder.gen[Type]
  given typeEncoder: JsonEncoder[Type] = DeriveJsonEncoder.gen[Type]

  def parse(typeName: String): Either[ParseError, Type] =
    Type.values.find(_.typeName == typeName).toRight(ParseError(s"Unknown type: $typeName"))

}
