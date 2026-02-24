package is.valsk.trmnlhomescreen.homeassistant.message.model.responses

import is.valsk.trmnlhomescreen.homeassistant.message.model.HassResponseMessage
import zio.json.{DeriveJsonDecoder, JsonDecoder}

case class AuthOK(
    `type`: String,
    ha_version: String,
) extends HassResponseMessage

object AuthOK {
  given decoder: JsonDecoder[AuthOK] = DeriveJsonDecoder.gen[AuthOK]
}
