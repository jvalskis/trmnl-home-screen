package is.valsk.trmnlhomescreen.homeassistant

import is.valsk.trmnlhomescreen.PropertiesExtractor
import zio.{RLayer, UIO, URLayer, ZIO, ZLayer}

import scala.jdk.CollectionConverters.*

object HomeAssistantPropertiesExtractor:

  val layer: URLayer[HomeAssistantConfig & HomeAssistantStateRepository, PropertiesExtractor] = ZLayer {
    for {
      repository <- ZIO.service[HomeAssistantStateRepository]
      config <- ZIO.service[HomeAssistantConfig]
    } yield new PropertiesExtractor:
      def extract: UIO[Map[String, Any]] =
        repository.get.map { entities =>
          Seq(
            "homeassistant_enabled" -> config.enabled,
            "entities" -> entities.toSeq.map { (entityId, entity) =>
              Map(
                entityId -> Map[String, Any](
                  "entity_id" -> entity.entityId,
                  "friendly_name" -> entity.attributes.friendlyName.getOrElse(entity.entityId),
                  "state" -> entity.state,
                  "unit" -> entity.attributes.unitOfMeasurement.getOrElse(""),
                ).asJava,
              ).asJava
            }.asJava,
          ).toMap
        }
  }

  val configuredLayer: RLayer[HomeAssistantStateRepository, PropertiesExtractor] = HomeAssistantConfig.layer >>> layer
