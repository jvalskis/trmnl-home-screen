package is.valsk.trmnlhomescreen.homeassistant.message.model.responses

import Result.HassError
import is.valsk.trmnlhomescreen.homeassistant.message.model.{HassIdentifiableMessage, HassResponseMessage}
import zio.json.{DeriveJsonDecoder, JsonDecoder}
import zio.json.ast.Json

case class Result(
    `type`: String,
    id: Int,
    success: Boolean,
    result: Option[Json],
    error: Option[HassError],
) extends HassResponseMessage
    with HassIdentifiableMessage

object Result {
  given hassErrorDecoder: JsonDecoder[HassError] = DeriveJsonDecoder.gen[HassError]
  given decoder: JsonDecoder[Result] = DeriveJsonDecoder.gen[Result]

  case class HassError(
      code: String,
      message: String,
  )

}
