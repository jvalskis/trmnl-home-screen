package is.valsk.trmnlhomescreen.hass.messages.responses

import is.valsk.trmnlhomescreen.hass.messages.HassResponseMessage
import zio.json.{DeriveJsonDecoder, JsonDecoder}

case class AuthRequired(
    `type`: String,
    ha_version: String,
) extends HassResponseMessage

object AuthRequired {
  given decoder: JsonDecoder[AuthRequired] = DeriveJsonDecoder.gen[AuthRequired]
}