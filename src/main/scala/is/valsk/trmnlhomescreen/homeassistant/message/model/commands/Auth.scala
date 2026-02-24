package is.valsk.trmnlhomescreen.homeassistant.message.model.commands

import is.valsk.trmnlhomescreen.homeassistant.message.Type
import is.valsk.trmnlhomescreen.homeassistant.message.model.HassRequestMessage
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
