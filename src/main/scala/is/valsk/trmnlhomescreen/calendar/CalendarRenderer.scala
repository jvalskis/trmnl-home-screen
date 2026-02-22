package is.valsk.trmnlhomescreen.calendar

import liqp.{Template, TemplateParser}
import zio.*

import java.nio.file.{Files, Path}
import java.time.format.DateTimeFormatter
import scala.jdk.CollectionConverters.*

trait CalendarRenderer:
  def render(events: List[CalendarEvent]): Task[String]

object CalendarRenderer:

  val configuredLayer: ZLayer[Any, Throwable, CalendarRenderer] = CalendarConfig.layer >>> layer

  val layer: ZLayer[CalendarConfig, Throwable, CalendarRenderer] =
    ZLayer.fromZIO {
      for
        config <- ZIO.service[CalendarConfig]
        renderer <- ZIO
          .attempt {
            val path = config.templateFile
            val content = Files.readString(Path.of(path))
            val parser = new TemplateParser.Builder().build()
            val template = parser.parse(content)
            LiveCalendarRenderer(template)
          }
          .tapError(e => ZIO.logError(s"Failed to load template from ${config.templateFile}: ${e.getMessage}"))
      yield renderer
    }

  private val displayFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")

  private final case class LiveCalendarRenderer(template: Template) extends CalendarRenderer:

    def render(events: List[CalendarEvent]): Task[String] =
      ZIO.attempt {
        val eventMaps = events.map { event =>
          Map[String, Any](
            "summary" -> event.summary,
            "start" -> event.dtStart.format(displayFormatter),
            "end" -> event.dtEnd.map(_.format(displayFormatter)).getOrElse(""),
            "location" -> event.location.getOrElse(""),
            "description" -> event.description.getOrElse(""),
          ).asJava
        }.asJava
        val variables = Map[String, Any](
          "events" -> eventMaps,
          "event_count" -> events.size,
        )
        template.render(variables.asJava)
      }
