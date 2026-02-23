package is.valsk.trmnlhomescreen.hass

import liqp.{Template, TemplateParser}
import zio.*

import java.nio.file.{Files, Path}
import scala.jdk.CollectionConverters.*

trait HomeAssistantRenderer:
  def render(areaGroups: List[AreaGroup]): Task[String]

object HomeAssistantRenderer:

  val configuredLayer: ZLayer[Any, Throwable, HomeAssistantRenderer] = HomeAssistantConfig.layer >>> layer

  val layer: ZLayer[HomeAssistantConfig, Throwable, HomeAssistantRenderer] =
    ZLayer.fromZIO {
      for
        config <- ZIO.service[HomeAssistantConfig]
        renderer <- ZIO
          .attempt {
            val path = config.templateFile
            val content = Files.readString(Path.of(path))
            val parser = new TemplateParser.Builder().build()
            val template = parser.parse(content)
            LiveHomeAssistantRenderer(template)
          }
          .tapError(e => ZIO.logError(s"Failed to load template from ${config.templateFile}: ${e.getMessage}"))
      yield renderer
    }

  private final case class LiveHomeAssistantRenderer(template: Template) extends HomeAssistantRenderer:

    def render(areaGroups: List[AreaGroup]): Task[String] =
      ZIO.attempt {
        val totalEntities = areaGroups.map(_.entities.size).sum
        val areaMaps = areaGroups.map { group =>
          val entityMaps = group.entities.map { entity =>
            Map[String, Any](
              "entity_id" -> entity.entityId,
              "friendly_name" -> entity.friendlyName,
              "state" -> entity.state,
              "unit" -> entity.unitOfMeasurement,
            ).asJava
          }.asJava
          Map[String, Any](
            "area_name" -> group.areaName,
            "entities" -> entityMaps,
            "entity_count" -> group.entities.size,
          ).asJava
        }.asJava
        val variables = Map[String, Any](
          "areas" -> areaMaps,
          "area_count" -> areaGroups.size,
          "total_entities" -> totalEntities,
        )
        template.render(variables.asJava)
      }
