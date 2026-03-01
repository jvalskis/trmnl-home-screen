package is.valsk.trmnlhomescreen.homeassistant

import is.valsk.trmnlhomescreen.PropertiesExtractor
import zio.{RLayer, UIO, URLayer, ZIO, ZLayer}

import scala.jdk.CollectionConverters.*

object HomeAssistantPropertiesExtractor:

  val layer
      : URLayer[HomeAssistantConfig & HomeAssistantStateRepository & HomeAssistantAreaRepository, PropertiesExtractor] =
    ZLayer {
      for {
        repository <- ZIO.service[HomeAssistantStateRepository]
        areaRepository <- ZIO.service[HomeAssistantAreaRepository]
        config <- ZIO.service[HomeAssistantConfig]
      } yield new PropertiesExtractor:
        def extract: UIO[Map[String, Any]] =
          for {
            entities <- repository.get
            areasByEntity <- ZIO.foreach(entities)((entityId, _) =>
              areaRepository.getAreaForEntity(entityId).map(entityId -> _),
            )
          } yield Seq(
            "homeassistant_enabled" -> config.enabled,
            "entities" -> entities.map { (entityId, entity) =>
              entityId -> Map[String, Any](
                "entity_id" -> entity.entityId,
                "friendly_name" -> entity.attributes.friendlyName.getOrElse(entity.entityId),
                "state" -> entity.state,
                "unit" -> entity.attributes.unitOfMeasurement.getOrElse(""),
                "area" -> areasByEntity(entityId).getOrElse(""),
              ).asJava
            }.asJava,
          ).toMap
    }

  val configuredLayer: RLayer[HomeAssistantStateRepository & HomeAssistantAreaRepository, PropertiesExtractor] =
    HomeAssistantConfig.layer >>> layer
