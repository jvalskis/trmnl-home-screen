package is.valsk.trmnlhomescreen.homeassistant

import is.valsk.trmnlhomescreen.homeassistant.message.model.responses.Event.EntityState
import liqp.{Template, TemplateParser}
import zio.*

import java.nio.file.{Files, Path}
import scala.jdk.CollectionConverters.*

trait HomeAssistantRenderer:
  def render(entities: Map[String, EntityState]): Task[String]

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

    def render(entities: Map[String, EntityState]): Task[String] =
      ZIO.attempt {
        val entityMaps = entities.values.toList.sortBy(_.entityId).map { entity =>
          Map[String, Any](
            "entity_id" -> entity.entityId,
            "friendly_name" -> entity.attributes.friendlyName.getOrElse(entity.entityId),
            "state" -> entity.state,
            "unit" -> entity.attributes.unitOfMeasurement.getOrElse(""),
          ).asJava
        }.asJava
        val variables = Map[String, Any](
          "entities" -> entityMaps,
          "entity_count" -> entities.size,
        )
        template.render(variables.asJava)
      }
