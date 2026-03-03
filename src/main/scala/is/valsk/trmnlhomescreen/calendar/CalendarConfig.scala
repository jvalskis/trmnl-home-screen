package is.valsk.trmnlhomescreen.calendar

import is.valsk.trmnlhomescreen.Configs.makeLayer
import zio.*

import java.time.ZoneId

final case class CalendarConfig(
    enabled: Boolean,
    calendarUrl: String,
    authType: String,
    username: String,
    password: String,
    fetchIntervalMinutes: Int,
    daysAhead: Int,
    timeZone: String,
) {
  def getZoneId: ZoneId = ZoneId.of(timeZone)
}

object CalendarConfig:

  val layer: ZLayer[Any, Config.Error, CalendarConfig] = makeLayer("calendar")
