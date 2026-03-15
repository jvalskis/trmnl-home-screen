package is.valsk.trmnlhomescreen.weather_api

import is.valsk.trmnlhomescreen.Configs.makeLayer
import zio.*

final case class WeatherApiConfig(
    enabled: Boolean,
    apiKey: String,
    city: String,
    fetchIntervalMinutes: Int,
    forecastDays: Int,
)

object WeatherApiConfig:

  val layer: ZLayer[Any, Config.Error, WeatherApiConfig] = makeLayer("weatherApi")
