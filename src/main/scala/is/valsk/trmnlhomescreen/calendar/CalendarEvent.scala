package is.valsk.trmnlhomescreen.calendar

import zio.json.jsonField

import java.time.LocalDateTime

final case class CalendarEvent(
    summary: String,
    @jsonField("dtStart")
    startDate: LocalDateTime,
    @jsonField("dtEnd")
    endDate: Option[LocalDateTime],
    location: Option[String],
    description: Option[String],
)

object CalendarEvent:
  val DefaultSummary = "(No title)"
