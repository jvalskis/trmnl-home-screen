package is.valsk.trmnlhomescreen

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

  private val descriptor: Config[WeatherConfig] =
    deriveConfig[WeatherConfig].nested("weather")

  val layer: ZLayer[Any, Config.Error, WeatherConfig] =
    ZLayer.fromZIO(
      TypesafeConfigProvider.fromResourcePath().load(descriptor),
    )
