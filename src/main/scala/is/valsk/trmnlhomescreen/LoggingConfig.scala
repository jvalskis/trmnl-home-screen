package is.valsk.trmnlhomescreen

import is.valsk.trmnlhomescreen.Configs.makeLayer
import zio.*

final case class LoggingConfig(
    level: String,
)

object LoggingConfig:

  val layer: ZLayer[Any, Config.Error, LoggingConfig] = makeLayer("logging")
