package is.valsk.trmnlhomescreen.calendar

import is.valsk.trmnlhomescreen.PropertiesExtractor
import is.valsk.trmnlhomescreen.PropertiesExtractor.{MapProperty, PropertyEntry, asScalar}
import zio.{RLayer, UIO, URLayer, ZIO, ZLayer}

import java.time.format.DateTimeFormatter
import java.time.{LocalDate, LocalDateTime, ZoneId}
import scala.jdk.CollectionConverters.*

object CalendarPropertiesExtractor:

  private val displayFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")

  private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

  val layer: URLayer[CalendarConfig & CalendarStateRepository, PropertiesExtractor] = ZLayer {
    for {
      repository <- ZIO.service[CalendarStateRepository]
      config <- ZIO.service[CalendarConfig]
    } yield new PropertiesExtractor:
      def extract: UIO[MapProperty] = {
        val currentDate = LocalDateTime.now(ZoneId.of("Europe/Vilnius"))
        repository.get.map { events =>
          MapProperty(
            "calendar_enabled" -> config.enabled.asScalar,
            "events" -> events.sortBy(_.startDate).map(createEventInfo(currentDate)).asList,
            "events_by_day" -> (0 to config.daysAhead)
              .map { daysSinceToday =>
                val date = currentDate.plusDays(daysSinceToday).toLocalDate
                MapProperty(
                  "days_since_today" -> daysSinceToday.asScalar,
                  "events" -> events.filter(_.happensOnDay(date)).map(createEventInfo(currentDate)).asList,
                  "date" -> date.asScalar,
                  "day_of_week" -> date.getDayOfWeek.asScalar,
                )
              }
              .asList,
          )
        }

      }
  }

  extension (event: CalendarEvent) {

    private def happensOnDay(day: LocalDate) = {
      (event.startDate.toLocalDate.isEqual(day) || event.startDate.toLocalDate.isBefore(day)) && (event.endDate.exists(
        _.toLocalDate.isEqual(day),
      ) || event.endDate.exists(_.toLocalDate.isAfter(day)))
    }

    private def startsOn(day: LocalDate) = {
      event.startDate.toLocalDate == day
    }

    private def endsOn(day: LocalDate) = {
      event.endDate.map(_.toLocalDate).contains(day)
    }

  }

  private def createEventInfo(currentTime: LocalDateTime)(event: CalendarEvent): PropertyEntry =
    MapProperty(
      "summary" -> event.summary.asScalar,
      "start" -> event.startDate.format(displayFormatter).asScalar,
      "start_time" -> Some(event.startDate).filter(d => event.startsOn(currentTime.toLocalDate)).fold("")(_.format(timeFormatter)).asScalar,
      "end" -> event.endDate.fold("")(_.format(displayFormatter)).asScalar,
      "end_time" -> event.endDate.filter(d => event.endsOn(currentTime.toLocalDate)).fold("")(_.format(timeFormatter)).asScalar,
      "location" -> event.location.getOrElse("").asScalar,
      "description" -> event.description.getOrElse("").asScalar,
    )

  val configuredLayer: RLayer[CalendarStateRepository, PropertiesExtractor] = CalendarConfig.layer >>> layer
