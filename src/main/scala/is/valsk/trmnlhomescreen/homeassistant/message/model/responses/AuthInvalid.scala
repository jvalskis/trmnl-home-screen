package is.valsk.trmnlhomescreen.homeassistant.message.model.responses

import is.valsk.trmnlhomescreen.homeassistant.message.model.HassResponseMessage
import zio.json.{DeriveJsonDecoder, JsonDecoder}

case class AuthInvalid(
    `type`: String,
    message: String,
) extends HassResponseMessage

object AuthInvalid {
  given decoder: JsonDecoder[AuthInvalid] = DeriveJsonDecoder.gen[AuthInvalid]
}
