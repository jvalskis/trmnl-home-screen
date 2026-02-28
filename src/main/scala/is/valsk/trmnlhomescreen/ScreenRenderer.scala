package is.valsk.trmnlhomescreen

import is.valsk.trmnlhomescreen.calendar.CalendarEvent
import is.valsk.trmnlhomescreen.homeassistant.message.model.responses.Event.EntityState
import liqp.{Template, TemplateParser}
import zio.*

import java.nio.file.{Files, Path}
import java.time.format.DateTimeFormatter
import scala.jdk.CollectionConverters.*

trait ScreenRenderer:
  def render(state: ScreenState): Task[String]

object ScreenRenderer:

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

  private val displayFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")

  private final case class LiveScreenRenderer(template: Template) extends ScreenRenderer:

    def render(state: ScreenState): Task[String] =
      ZIO.attempt {
        // Calendar variables
        val calendarProperties = Seq(
          Nil :+ "has_calendar" -> state.calendarEvents.isDefined,
          state.calendarEvents.toSeq.map { events =>
            "events" -> events.map { event =>
              Map[String, Any](
                "summary" -> event.summary,
                "start" -> event.dtStart.format(displayFormatter),
                "end" -> event.dtEnd.fold("")(_.format(displayFormatter)),
                "location" -> event.location.getOrElse(""),
                "description" -> event.description.getOrElse(""),
              ).asJava
            }.asJava
          },
        ).flatten

        // Weather variables
        val weatherProperties = Seq(
          Nil :+ "has_weather" -> state.weatherConditions.isDefined,
          state.weatherConditions.toSeq.flatMap { conditions =>
            Seq(
              Nil :+ "weather_text" -> conditions.weatherText,
              Nil :+ "weather_icon" -> conditions.weatherIcon,
              Nil :+ "temp_metric_value" -> conditions.temperature.metric.value,
              Nil :+ "temp_metric_unit" -> conditions.temperature.metric.unit,
              Nil :+ "temp_imperial_value" -> conditions.temperature.imperial.value,
              Nil :+ "temp_imperial_unit" -> conditions.temperature.imperial.unit,
              Nil :+ "has_precipitation" -> conditions.hasPrecipitation,
              Nil :+ "is_day_time" -> conditions.isDayTime,
              Nil :+ "observation_time" -> conditions.localObservationDateTime,
              conditions.relativeHumidity.toSeq.map("relative_humidity" -> _),
              conditions.cloudCover.toSeq.map("cloud_cover" -> _),
              conditions.uvIndex.toSeq.map("uv_index" -> _),
              conditions.uvIndexText.toSeq.map("uv_index_text" -> _),
              conditions.wind.toSeq.flatMap { wind =>
                Seq(
                  "wind_speed_metric_value" -> wind.speed.metric.value,
                  "wind_speed_metric_unit" -> wind.speed.metric.unit,
                  "wind_speed_imperial_value" -> wind.speed.imperial.value,
                  "wind_speed_imperial_unit" -> wind.speed.imperial.unit,
                  "wind_direction" -> wind.direction.localized,
                  "wind_direction_degrees" -> wind.direction.degrees,
                )
              },
              conditions.visibility.toSeq.flatMap { vis =>
                Seq(
                  "visibility_metric_value" -> vis.metric.value,
                  "visibility_metric_unit" -> vis.metric.unit,
                  "visibility_imperial_value" -> vis.imperial.value,
                  "visibility_imperial_unit" -> vis.imperial.unit,
                )
              },
              conditions.realFeelTemperature.toSeq.flatMap { realFeel =>
                Seq(
                  "real_feel_metric_value" -> realFeel.metric.value,
                  "real_feel_metric_unit" -> realFeel.metric.unit,
                  "real_feel_imperial_value" -> realFeel.imperial.value,
                  "real_feel_imperial_unit" -> realFeel.imperial.unit,
                )
              },
            ).flatten
          },
        ).flatten

        // Home Assistant entity variables
        val homeAssistantProperties = Seq(
          Nil :+ "has_entities" -> state.entityStates.isDefined,
          state.entityStates.toSeq.map { entities =>
            "entities" -> entities.values.toList.sortBy(_.entityId).map { entity =>
              entity.entityId ->
                Map[String, Any](
                  "entity_id" -> entity.entityId,
                  "friendly_name" -> entity.attributes.friendlyName.getOrElse(entity.entityId),
                  "state" -> entity.state,
                  "unit" -> entity.attributes.unitOfMeasurement.getOrElse(""),
                ).asJava
            }.toMap.asJava
          },
        ).flatten

        val variables = (calendarProperties ++ weatherProperties ++ homeAssistantProperties).toMap
        template.render(variables.asJava)
      }
