package is.valsk.trmnlhomescreen.weather

import is.valsk.trmnlhomescreen.Configs.makeLayer
import zio.*

final case class WeatherConfig(
    enabled: Boolean,
    apiKey: String,
    city: String,
    fetchIntervalMinutes: Int,
)

object WeatherConfig:

  val layer: ZLayer[Any, Config.Error, WeatherConfig] = makeLayer("weather")
