package is.valsk.trmnlhomescreen.weather

import is.valsk.trmnlhomescreen.Configs.makeLayer
import zio.*
import zio.config.magnolia.deriveConfig
import zio.config.typesafe.TypesafeConfigProvider

final case class WeatherConfig(
    enabled: Boolean,
    apiKey: String,
    city: String,
    fetchIntervalMinutes: Int,
    templateFile: String,
)

object WeatherConfig:

  val layer: ZLayer[Any, Config.Error, WeatherConfig] = makeLayer("weather")