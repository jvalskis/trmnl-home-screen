package is.valsk.trmnlhomescreen.calendar

import is.valsk.trmnlhomescreen.PropertiesExtractor
import is.valsk.trmnlhomescreen.PropertiesExtractor.{MapProperty, PropertyEntry, asScalar}
import zio.{RLayer, UIO, URLayer, ZIO, ZLayer}

import java.time.{LocalDate, ZoneId}
import java.time.format.DateTimeFormatter
import java.time.*

object CalendarPropertiesExtractor:

  private val displayFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")

  private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

  val layer: URLayer[CalendarConfig & CalendarStateRepository, PropertiesExtractor] = ZLayer {
    for {
      repository <- ZIO.service[CalendarStateRepository]
      config <- ZIO.service[CalendarConfig]
    } yield new PropertiesExtractor:
      def extract: UIO[MapProperty] = {
        val currentDate = Instant.now().atZone(config.getZoneId)
        repository.get.map { events =>
          MapProperty(
            "calendar_enabled" -> config.enabled.asScalar,
            "events" -> events.sortBy(_.startDate).map(createBaseEventInfo(config)).asList,
            "events_by_day" -> (0 to config.daysAhead)
              .map { daysSinceToday =>
                val date = currentDate.plusDays(daysSinceToday).toLocalDate
                MapProperty(
                  "days_since_today" -> daysSinceToday.asScalar,
                  "events" -> events.filter(_.happensOnDay(date)).sortBy(_.startDate).map(
                    createEventInfo(config, daysSinceToday, date, currentDate),
                  ).asList,
                  "date" -> date.asScalar,
                  "day_of_week" -> date.getDayOfWeek.getValue.asScalar,
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

  }

  private def createEventInfo(event: CalendarEvent): PropertyEntry =
    MapProperty(
      "summary" -> event.summary.asScalar,
      "start" -> event.startDate.format(displayFormatter).asScalar,
      "end" -> event.endDate.fold("")(_.format(displayFormatter)).asScalar,
      "location" -> event.location.getOrElse("").asScalar,
      "description" -> event.description.getOrElse("").asScalar,
    )

  val configuredLayer: RLayer[CalendarStateRepository, PropertiesExtractor] = CalendarConfig.layer >>> layer

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

  private def createEventInfo(config: CalendarConfig, daysSinceToday: Int, date: LocalDate, currentTime: ZonedDateTime)(
      event: CalendarEvent,
  ): PropertyEntry = {
    val currentDay = currentTime.toLocalDate
    val (startDate, maybeEndDate) = getEventDates(config, event)
    createBaseEventInfo(config)(event) ++ MapProperty(
      "start_time" -> Some(startDate).filter(d => event.startsOn(date)).fold("")(
        _.format(timeFormatter),
      ).asScalar,
      "end_time" -> maybeEndDate.filter(d => event.endsOn(date)).fold("")(
        _.format(timeFormatter),
      ).asScalar,
      "status" -> resolveStatus(config, daysSinceToday, currentTime, startDate, maybeEndDate).asScalar,
      "day" -> currentDay.asScalar,
    )
  }

  private def getEventDates(config: CalendarConfig, event: CalendarEvent) = {
    val eventStartDate = event.startDate.convertToZonedTime(config.getZoneId)
    val maybeEventEndDate = event.endDate.map(_.convertToZonedTime(config.getZoneId))
    (eventStartDate, maybeEventEndDate)
  }

  private def createBaseEventInfo(
      config: CalendarConfig,
  )(
      event: CalendarEvent,
  ): MapProperty = {
    val (startDate, maybeEndDate) = getEventDates(config, event)
    MapProperty(
      "summary" -> event.summary.asScalar,
      "start" -> startDate.format(displayFormatter).asScalar,
      "end" -> maybeEndDate.fold("")(_.format(displayFormatter)).asScalar,
      "location" -> event.location.getOrElse("").asScalar,
      "description" -> event.description.getOrElse("").asScalar,
    )
  }

  private def resolveStatus(
      config: CalendarConfig,
      daysSinceToday: Int,
      currentTime: ZonedDateTime,
      startDate: ZonedDateTime,
      maybeEndDate: Option[ZonedDateTime],
  ) = {
    if (daysSinceToday == 0) {
      if (startDate.isBefore(currentTime) && maybeEndDate.forall(_.isAfter(currentTime))) {
        "in_progress"
      } else if (startDate.isBefore(currentTime) && maybeEndDate.forall(_.isBefore(currentTime))) {
        "ended"
      } else {
        "not_started"
      }
    } else {
      "not_started"
    }
  }

  extension (time: LocalDateTime) {

    private def convertToZonedTime(zoneId: ZoneId): ZonedDateTime =
      time.atZone(ZoneOffset.UTC).withZoneSameInstant(zoneId)

  }
