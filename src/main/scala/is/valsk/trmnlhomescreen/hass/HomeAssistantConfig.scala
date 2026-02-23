package is.valsk.trmnlhomescreen.hass

import zio.*
import zio.config.magnolia.deriveConfig
import zio.config.typesafe.TypesafeConfigProvider

final case class HomeAssistantConfig(
    enabled: Boolean,
    webSocketUrl: String,
    accessToken: String,
    subscribedEntityIds: List[String],
    templateFile: String,
)

object HomeAssistantConfig:

  private val descriptor: Config[HomeAssistantConfig] =
    deriveConfig[HomeAssistantConfig].nested("homeAssistant")

  val layer: ZLayer[Any, Config.Error, HomeAssistantConfig] =
    ZLayer.fromZIO(
      TypesafeConfigProvider.fromResourcePath().load(descriptor),
    )
