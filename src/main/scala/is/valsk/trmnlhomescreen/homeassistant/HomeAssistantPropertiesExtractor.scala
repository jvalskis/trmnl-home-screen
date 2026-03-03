package is.valsk.trmnlhomescreen.homeassistant

import is.valsk.trmnlhomescreen.PropertiesExtractor
import is.valsk.trmnlhomescreen.PropertiesExtractor.{MapProperty, asScalar}
import zio.{RLayer, UIO, URLayer, ZIO, ZLayer}

object HomeAssistantPropertiesExtractor:

  val layer
      : URLayer[HomeAssistantConfig & HomeAssistantStateRepository & HomeAssistantAreaRepository, PropertiesExtractor] =
    ZLayer {
      for {
        repository <- ZIO.service[HomeAssistantStateRepository]
        areaRepository <- ZIO.service[HomeAssistantAreaRepository]
        config <- ZIO.service[HomeAssistantConfig]
      } yield new PropertiesExtractor:
        def extract: UIO[MapProperty] =
          for {
            entities <- repository.get
            areasByEntity <- ZIO.foreach(entities)((entityId, _) =>
              areaRepository.getAreaForEntity(entityId).map(entityId -> _),
            )
            _ <- ZIO.logDebug(s"Areas: $areasByEntity") *> ZIO.logDebug(s"Entities: $entities")
          } yield MapProperty(
            "homeassistant_enabled" -> config.enabled.asScalar,
            "entities" -> entities
              .map { (entityId, entity) =>
                entityId -> MapProperty(
                  "entity_id" -> entity.entityId.asScalar,
                  "friendly_name" -> entity.attributes.friendlyName.getOrElse(entity.entityId).asScalar,
                  "state" -> entity.state.asScalar,
                  "unit" -> entity.attributes.unitOfMeasurement.getOrElse("").asScalar,
                  "area" -> areasByEntity.get(entityId).flatten.getOrElse("").asScalar,
                )
              }
              .asMap,
          )
    }

  val configuredLayer: RLayer[HomeAssistantStateRepository & HomeAssistantAreaRepository, PropertiesExtractor] =
    HomeAssistantConfig.layer >>> layer
