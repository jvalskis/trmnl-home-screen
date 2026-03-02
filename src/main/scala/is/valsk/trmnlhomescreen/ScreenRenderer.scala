package is.valsk.trmnlhomescreen

import is.valsk.trmnlhomescreen.PropertiesExtractor.{MapProperty, PropertyEntry}
import liqp.{Template, TemplateParser}
import zio.*

import java.nio.file.{Files, Path}
import scala.jdk.CollectionConverters.*

trait ScreenRenderer:
  def render(properties: MapProperty): Task[String]

object ScreenRenderer:

  private final case class LiveScreenRenderer(template: Template) extends ScreenRenderer:

    def render(properties: MapProperty): Task[String] = {
      val renderProperties = properties.asJava
      ZIO.logDebug(s"Render properties: ${renderProperties.toString}") *> ZIO.attempt {
        template.render(renderProperties)
      }
    }

  val layer: ZLayer[ScreenConfig, Throwable, ScreenRenderer] =
    ZLayer.fromZIO {
      for
        config <- ZIO.service[ScreenConfig]
        renderer <- ZIO
          .attempt {
            val content = Files.readString(Path.of(config.templateFile))
            val parser = new TemplateParser.Builder().build()
            val template = parser.parse(content)
            LiveScreenRenderer(template)
          }
          .tapError(e => ZIO.logError(s"Failed to load template from ${config.templateFile}: ${e.getMessage}"))
      yield renderer
    }

  val configuredLayer: ZLayer[Any, Throwable, ScreenRenderer] = ScreenConfig.layer >>> layer
