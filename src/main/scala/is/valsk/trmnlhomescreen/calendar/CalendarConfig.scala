package is.valsk.trmnlhomescreen.calendar

import is.valsk.trmnlhomescreen.Configs.makeLayer
import zio.*
import zio.config.magnolia.deriveConfig
import zio.config.typesafe.TypesafeConfigProvider

final case class CalendarConfig(
    enabled: Boolean,
    calendarUrl: String,
    authType: String,
    username: String,
    password: String,
    fetchIntervalMinutes: Int,
    daysAhead: Int,
    templateFile: String,
)

object CalendarConfig:

  val layer: ZLayer[Any, Config.Error, CalendarConfig] = makeLayer("calendar")
