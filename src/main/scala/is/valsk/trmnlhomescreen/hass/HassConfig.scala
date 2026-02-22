package is.valsk.trmnlhomescreen.hass

import is.valsk.trmnlhomescreen.Configs.makeLayer
import zio.{Config, Layer}

case class HassConfig(
    webSocketUrl: String,
    accessToken: String,
)

object HassConfig:

  val layer: Layer[Config.Error, HassConfig] = makeLayer("hass")