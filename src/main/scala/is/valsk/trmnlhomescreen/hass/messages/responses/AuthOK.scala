package is.valsk.trmnlhomescreen.hass.messages.responses

import is.valsk.trmnlhomescreen.hass.messages.HassResponseMessage
import zio.json.{DeriveJsonDecoder, JsonDecoder}

case class AuthOK(
    `type`: String,
    ha_version: String,
) extends HassResponseMessage

object AuthOK {
  given decoder: JsonDecoder[AuthOK] = DeriveJsonDecoder.gen[AuthOK]
}