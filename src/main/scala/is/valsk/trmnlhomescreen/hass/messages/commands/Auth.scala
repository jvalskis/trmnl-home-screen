package is.valsk.trmnlhomescreen.hass.messages.commands

import is.valsk.trmnlhomescreen.hass.messages.{HassRequestMessage, Type}
import zio.json.{DeriveJsonEncoder, JsonEncoder}

case class Auth(
    `type`: String,
    access_token: String
) extends HassRequestMessage

object Auth {
  given encoder: JsonEncoder[Auth] = DeriveJsonEncoder.gen[Auth]

  def apply(accessToken: String): Auth = Auth(Type.Auth.typeName, accessToken)
}
