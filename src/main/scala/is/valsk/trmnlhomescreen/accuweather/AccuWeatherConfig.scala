package is.valsk.trmnlhomescreen.accuweather

import is.valsk.trmnlhomescreen.Configs.makeLayer
import zio.*

final case class AccuWeatherConfig(
    enabled: Boolean,
    apiKey: String,
    city: String,
    fetchIntervalMinutes: Int,
)

object AccuWeatherConfig:

  val layer: ZLayer[Any, Config.Error, AccuWeatherConfig] = makeLayer("weather")
