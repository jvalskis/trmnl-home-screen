package is.valsk.trmnlhomescreen.calendar

import is.valsk.trmnlhomescreen.PropertiesExtractor
import zio.{RLayer, UIO, URLayer, ZIO, ZLayer}

import java.time.{LocalDate, ZoneId}
import java.time.format.DateTimeFormatter
import scala.jdk.CollectionConverters.*

object CalendarPropertiesExtractor:

  private val displayFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")

  val layer: URLayer[CalendarConfig & CalendarStateRepository, PropertiesExtractor] = ZLayer {
    for {
      repository <- ZIO.service[CalendarStateRepository]
      config <- ZIO.service[CalendarConfig]
    } yield new PropertiesExtractor:
      def extract: UIO[Map[String, Any]] = {
        val currentDate = LocalDate.now(ZoneId.of("Europe/Vilnius"))
        repository.get.map { events =>
          Seq(
            "calendar_enabled" -> config.enabled,
            "events" -> events.sortBy(_.startDate).map(createEventInfo(_).asJava).asJava,
            "events_by_day" -> (0 to config.daysAhead).map { daysSinceToday =>
              val date = currentDate.plusDays(daysSinceToday)
              Map(
                "days_since_today" -> daysSinceToday,
                "events" -> events.filter(_.happensOnDay(date)).map(createEventInfo(_).asJava).asJava,
                "date" -> date,
                "day_of_week" -> date.getDayOfWeek,
              ).asJava
            }.asJava,
          ).toMap
        }

      }
  }

  extension (event: CalendarEvent) {
    private def happensOnDay(day: LocalDate) = {
      (event.startDate.toLocalDate.isEqual(day) || event.startDate.toLocalDate.isBefore(day)) && (event.endDate.exists(_.toLocalDate.isEqual(day)) || event.endDate.exists(_.toLocalDate.isAfter(day)))
    }
  }

  private def createEventInfo(event: CalendarEvent): Map[String, Any] =
    Map(
      "summary" -> event.summary,
      "start" -> event.startDate.format(displayFormatter),
      "end" -> event.endDate.fold("")(_.format(displayFormatter)),
      "location" -> event.location.getOrElse(""),
      "description" -> event.description.getOrElse(""),
    )

  val configuredLayer: RLayer[CalendarStateRepository, PropertiesExtractor] = CalendarConfig.layer >>> layer
