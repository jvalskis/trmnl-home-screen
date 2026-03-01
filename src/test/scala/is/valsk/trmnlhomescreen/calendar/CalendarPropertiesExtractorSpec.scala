package is.valsk.trmnlhomescreen.calendar

import is.valsk.trmnlhomescreen.PropertiesExtractor
import zio.*
import zio.test.*

import java.time.LocalDateTime
import scala.jdk.CollectionConverters.*

object CalendarPropertiesExtractorSpec extends ZIOSpecDefault:

  private val enabledConfig = CalendarConfig(
    enabled = true,
    calendarUrl = "http://localhost",
    authType = "none",
    username = "",
    password = "",
    fetchIntervalMinutes = 5,
    daysAhead = 7,
  )

  private val testLayer =
    CalendarStateRepository.layer ++ ZLayer.succeed(enabledConfig) >>> (CalendarStateRepository.layer ++ CalendarPropertiesExtractor.layer)

  private def withEvents(events: List[CalendarEvent]) =
    for
      repo <- ZIO.service[CalendarStateRepository]
      _ <- repo.update(events)
      extractor <- ZIO.service[PropertiesExtractor]
      result <- extractor.extract
    yield result

  def spec = suite("CalendarPropertiesExtractor")(
    test("includes calendar_enabled from config") {
      (for
        extractor <- ZIO.service[PropertiesExtractor]
        result <- extractor.extract
      yield assertTrue(result("calendar_enabled") == true))
        .provide(testLayer)
    },
    test("calendar_enabled is false when config disabled") {
      val disabledConfig = enabledConfig.copy(enabled = false)
      val layer = CalendarStateRepository.layer ++ ZLayer.succeed(disabledConfig) >>>
        (CalendarStateRepository.layer ++ CalendarPropertiesExtractor.layer)
      (for
        extractor <- ZIO.service[PropertiesExtractor]
        result <- extractor.extract
      yield assertTrue(result("calendar_enabled") == false))
        .provide(layer)
    },
    test("returns empty events list when repository is empty") {
      (for
        extractor <- ZIO.service[PropertiesExtractor]
        result <- extractor.extract
        events = result("events").asInstanceOf[java.util.List[?]]
      yield assertTrue(events.isEmpty))
        .provide(testLayer)
    },
    test("extracts event summary") {
      val event = CalendarEvent(
        summary = "Team Standup",
        dtStart = LocalDateTime.of(2026, 3, 1, 9, 0),
        dtEnd = Some(LocalDateTime.of(2026, 3, 1, 9, 30)),
        location = None,
        description = None,
      )
      withEvents(List(event))
        .map { result =>
          val events = result("events").asInstanceOf[java.util.List[java.util.Map[String, Any]]]
          val first = events.get(0)
          assertTrue(first.get("summary") == "Team Standup")
        }
        .provide(testLayer)
    },
    test("formats start and end times") {
      val event = CalendarEvent(
        summary = "Meeting",
        dtStart = LocalDateTime.of(2026, 1, 15, 14, 30),
        dtEnd = Some(LocalDateTime.of(2026, 1, 15, 15, 0)),
        location = None,
        description = None,
      )
      withEvents(List(event))
        .map { result =>
          val events = result("events").asInstanceOf[java.util.List[java.util.Map[String, Any]]]
          val first = events.get(0)
          assertTrue(
            first.get("start") == "2026-01-15 14:30",
            first.get("end") == "2026-01-15 15:00",
          )
        }
        .provide(testLayer)
    },
    test("end is empty string when dtEnd is None") {
      val event = CalendarEvent(
        summary = "All Day",
        dtStart = LocalDateTime.of(2026, 3, 1, 0, 0),
        dtEnd = None,
        location = None,
        description = None,
      )
      withEvents(List(event))
        .map { result =>
          val events = result("events").asInstanceOf[java.util.List[java.util.Map[String, Any]]]
          assertTrue(events.get(0).get("end") == "")
        }
        .provide(testLayer)
    },
    test("extracts location and description") {
      val event = CalendarEvent(
        summary = "Conference",
        dtStart = LocalDateTime.of(2026, 6, 10, 10, 0),
        dtEnd = None,
        location = Some("Room 42"),
        description = Some("Quarterly review"),
      )
      withEvents(List(event))
        .map { result =>
          val events = result("events").asInstanceOf[java.util.List[java.util.Map[String, Any]]]
          val first = events.get(0)
          assertTrue(
            first.get("location") == "Room 42",
            first.get("description") == "Quarterly review",
          )
        }
        .provide(testLayer)
    },
    test("location and description default to empty string when None") {
      val event = CalendarEvent(
        summary = "Quick sync",
        dtStart = LocalDateTime.of(2026, 3, 1, 12, 0),
        dtEnd = None,
        location = None,
        description = None,
      )
      withEvents(List(event))
        .map { result =>
          val events = result("events").asInstanceOf[java.util.List[java.util.Map[String, Any]]]
          val first = events.get(0)
          assertTrue(
            first.get("location") == "",
            first.get("description") == "",
          )
        }
        .provide(testLayer)
    },
    test("extracts multiple events in order") {
      val events = List(
        CalendarEvent("First", LocalDateTime.of(2026, 3, 1, 8, 0), None, None, None),
        CalendarEvent("Second", LocalDateTime.of(2026, 3, 1, 10, 0), None, None, None),
        CalendarEvent("Third", LocalDateTime.of(2026, 3, 1, 14, 0), None, None, None),
      )
      withEvents(events)
        .map { result =>
          val extracted = result("events").asInstanceOf[java.util.List[java.util.Map[String, Any]]]
          assertTrue(
            extracted.size() == 3,
            extracted.get(0).get("summary") == "First",
            extracted.get(1).get("summary") == "Second",
            extracted.get(2).get("summary") == "Third",
          )
        }
        .provide(testLayer)
    },
  )
