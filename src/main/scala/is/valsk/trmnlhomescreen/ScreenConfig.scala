package is.valsk.trmnlhomescreen

import is.valsk.trmnlhomescreen.Configs.makeLayer
import zio.*

final case class ScreenConfig(
    templateFile: String,
    renderIntervalSeconds: Int,
)

object ScreenConfig:

  val layer: ZLayer[Any, Config.Error, ScreenConfig] = makeLayer("screen")
