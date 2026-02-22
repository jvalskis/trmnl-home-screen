package is.valsk.trmnlhomescreen.calendar

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

  private val descriptor: Config[CalendarConfig] =
    deriveConfig[CalendarConfig].nested("calendar")

  val layer: ZLayer[Any, Config.Error, CalendarConfig] =
    ZLayer.fromZIO(
      TypesafeConfigProvider.fromResourcePath().load(descriptor),
    )
