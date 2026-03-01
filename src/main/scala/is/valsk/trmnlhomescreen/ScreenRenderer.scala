package is.valsk.trmnlhomescreen

import liqp.{Template, TemplateParser}
import zio.*

import java.nio.file.{Files, Path}
import scala.jdk.CollectionConverters.*

trait ScreenRenderer:
  def render(properties: Map[String, Any]): Task[String]

object ScreenRenderer:

  private final case class LiveScreenRenderer(template: Template) extends ScreenRenderer:

    def render(properties: Map[String, Any]): Task[String] =
      ZIO.attempt {
        template.render(properties.asJava)
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
