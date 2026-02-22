package is.valsk.trmnlhomescreen

import is.valsk.trmnlhomescreen.calendar.{CalDavClient, CalendarProgram, CalendarRenderer}
import is.valsk.trmnlhomescreen.weather.{AccuWeatherClient, WeatherProgram}
import zio.*
import zio.http.Client

object Main extends ZIOAppDefault:

  override def run: ZIO[Any, Any, Any] =
    val weatherProgram = ZIO.serviceWithZIO[WeatherProgram](_.run)
    val calendarProgram = ZIO.serviceWithZIO[CalendarProgram](_.run)

    (weatherProgram <&> calendarProgram).provide(
      Client.default,
      AccuWeatherClient.configuredLayer,
      TemplateRenderer.configuredLayer,
      CalDavClient.configuredLayer,
      CalendarRenderer.configuredLayer,
      WeatherProgram.configuredLayer,
      CalendarProgram.configuredLayer,
    )
