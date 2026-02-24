package is.valsk.trmnlhomescreen.homeassistant

import zio.*
import zio.config.magnolia.deriveConfig
import zio.config.typesafe.TypesafeConfigProvider

final case class HomeAssistantConfig(
    enabled: Boolean,
    webSocketUrl: String,
    accessToken: String,
    subscribedEntityIds: String,
    maxFrameSizeKb: Int,
    templateFile: String,
) {
  def subscribedEntityIdList: List[String] =
    subscribedEntityIds.split(",").map(_.trim).filter(_.nonEmpty).toList
}

object HomeAssistantConfig:

  private val descriptor: Config[HomeAssistantConfig] =
    deriveConfig[HomeAssistantConfig].nested("homeAssistant")

  val layer: ZLayer[Any, Config.Error, HomeAssistantConfig] =
    ZLayer.fromZIO(
      TypesafeConfigProvider.fromResourcePath().load(descriptor),
    )
