package is.valsk.trmnlhomescreen.calendar

import java.time.LocalDateTime

final case class CalendarEvent(
    summary: String,
    dtStart: LocalDateTime,
    dtEnd: Option[LocalDateTime],
    location: Option[String],
    description: Option[String],
)

object CalendarEvent:
  val DefaultSummary = "(No title)"
