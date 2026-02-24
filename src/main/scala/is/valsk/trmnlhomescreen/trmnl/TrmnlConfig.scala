package is.valsk.trmnlhomescreen.trmnl

import is.valsk.trmnlhomescreen.Configs.makeLayer
import zio.*

final case class TrmnlConfig(
    baseUrl: String,
    apiKey: String,
    macAddress: String,
)

object TrmnlConfig:

  val layer: ZLayer[Any, Config.Error, TrmnlConfig] = makeLayer("trmnl")
