package is.valsk.trmnlhomescreen.homeassistant.message.model.responses

import is.valsk.trmnlhomescreen.homeassistant.message.model.HassResponseMessage
import zio.json.{DeriveJsonDecoder, JsonDecoder}

case class AuthRequired(
    `type`: String,
    ha_version: String,
) extends HassResponseMessage

object AuthRequired {
  given decoder: JsonDecoder[AuthRequired] = DeriveJsonDecoder.gen[AuthRequired]
}
