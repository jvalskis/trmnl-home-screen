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

  val configuredLayer: ZLayer[Any, Throwable, ScreenRenderer] = ScreenConfig.layer >>> layer

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

  private val displayFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")

  private final case class LiveScreenRenderer(template: Template) extends ScreenRenderer:

    def render(state: ScreenState): Task[String] =
      ZIO.attempt {
        val variables = scala.collection.mutable.Map[String, Any]()

        // Calendar variables
        variables("has_calendar") = state.calendarEvents.isDefined
        state.calendarEvents.foreach { events =>
          val eventMaps = events.map { event =>
            Map[String, Any](
              "summary" -> event.summary,
              "start" -> event.dtStart.format(displayFormatter),
              "end" -> event.dtEnd.map(_.format(displayFormatter)).getOrElse(""),
              "location" -> event.location.getOrElse(""),
              "description" -> event.description.getOrElse(""),
            ).asJava
          }.asJava
          variables("events") = eventMaps
          variables("event_count") = events.size
        }

        // Weather variables
        variables("has_weather") = state.weatherConditions.isDefined
        state.weatherConditions.foreach { conditions =>
          variables("weather_text") = conditions.weatherText
          variables("weather_icon") = conditions.weatherIcon
          variables("temp_metric_value") = conditions.temperature.metric.value
          variables("temp_metric_unit") = conditions.temperature.metric.unit
          variables("temp_imperial_value") = conditions.temperature.imperial.value
          variables("temp_imperial_unit") = conditions.temperature.imperial.unit
          variables("has_precipitation") = conditions.hasPrecipitation
          variables("is_day_time") = conditions.isDayTime
          variables("observation_time") = conditions.localObservationDateTime

          conditions.relativeHumidity.foreach(v => variables("relative_humidity") = v)
          conditions.cloudCover.foreach(v => variables("cloud_cover") = v)
          conditions.uvIndex.foreach(v => variables("uv_index") = v)
          conditions.uvIndexText.foreach(v => variables("uv_index_text") = v)

          conditions.wind.foreach { wind =>
            variables("wind_speed_metric_value") = wind.speed.metric.value
            variables("wind_speed_metric_unit") = wind.speed.metric.unit
            variables("wind_speed_imperial_value") = wind.speed.imperial.value
            variables("wind_speed_imperial_unit") = wind.speed.imperial.unit
            variables("wind_direction") = wind.direction.localized
            variables("wind_direction_degrees") = wind.direction.degrees
          }

          conditions.visibility.foreach { vis =>
            variables("visibility_metric_value") = vis.metric.value
            variables("visibility_metric_unit") = vis.metric.unit
            variables("visibility_imperial_value") = vis.imperial.value
            variables("visibility_imperial_unit") = vis.imperial.unit
          }

          conditions.realFeelTemperature.foreach { realFeel =>
            variables("real_feel_metric_value") = realFeel.metric.value
            variables("real_feel_metric_unit") = realFeel.metric.unit
            variables("real_feel_imperial_value") = realFeel.imperial.value
            variables("real_feel_imperial_unit") = realFeel.imperial.unit
          }
        }

        // Home Assistant entity variables
        variables("has_entities") = state.entityStates.isDefined
        state.entityStates.foreach { entities =>
          val entityMaps = entities.values.toList.sortBy(_.entityId).map { entity =>
            Map[String, Any](
              "entity_id" -> entity.entityId,
              "friendly_name" -> entity.attributes.friendlyName.getOrElse(entity.entityId),
              "state" -> entity.state,
              "unit" -> entity.attributes.unitOfMeasurement.getOrElse(""),
            ).asJava
          }.asJava
          variables("entities") = entityMaps
          variables("entity_count") = entities.size
        }

        template.render(variables.asJava)
      }
