package is.valsk.trmnlhomescreen.hass.messages.commands

import is.valsk.trmnlhomescreen.hass.messages.{HassRequestMessage, Type}
import zio.json.{DeriveJsonEncoder, JsonEncoder, jsonField}

case class Auth(
  `type`: String,
  @jsonField("access_token")
    accessToken: String,
) extends HassRequestMessage

object Auth {
  given encoder: JsonEncoder[Auth] = DeriveJsonEncoder.gen[Auth]

  def apply(accessToken: String): Auth = Auth(Type.Auth.typeName, accessToken)
}
