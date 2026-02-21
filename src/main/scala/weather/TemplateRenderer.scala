package weather

import liqp.Template
import liqp.TemplateParser
import zio.*

import java.nio.file.{Files, Path}
import scala.jdk.CollectionConverters.*

trait TemplateRenderer:
  def render(conditions: CurrentConditions): Task[String]

object TemplateRenderer:

  val layer: ZLayer[WeatherConfig, Throwable, TemplateRenderer] =
    ZLayer.fromZIO {
      for
        config <- ZIO.service[WeatherConfig]
        renderer <- ZIO
          .attempt {
            val path = config.templateFile
            val content = Files.readString(Path.of(path))
            val parser = new TemplateParser.Builder().build()
            val template = parser.parse(content)
            LiveTemplateRenderer(template)
          }
          .tapError(e => ZIO.logError(s"Failed to load template from ${config.templateFile}: ${e.getMessage}"))
      yield renderer
    }

  private final case class LiveTemplateRenderer(template: Template) extends TemplateRenderer:

    def render(conditions: CurrentConditions): Task[String] =
      ZIO.attempt {
        val variables = Map[String, Any](
          "weather_text" -> conditions.weatherText,
          "temp_metric_value" -> conditions.temperature.metric.value,
          "temp_metric_unit" -> conditions.temperature.metric.unit,
          "temp_imperial_value" -> conditions.temperature.imperial.value,
          "temp_imperial_unit" -> conditions.temperature.imperial.unit,
          "has_precipitation" -> conditions.hasPrecipitation,
          "is_day_time" -> conditions.isDayTime,
          "observation_time" -> conditions.localObservationDateTime,
        )
        template.render(variables.asJava)
      }
