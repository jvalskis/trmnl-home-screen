package is.valsk.trmnlhomescreen

import is.valsk.trmnlhomescreen.calendar.{CalDavClient, CalendarConfig, CalendarProgram, CalendarRenderer}
import is.valsk.trmnlhomescreen.weather.{AccuWeatherClient, WeatherProgram}
import zio.*
import zio.http.Client

object Main extends ZIOAppDefault:

  override def run: ZIO[Any, Any, Any] =
    val weatherProgram = ZIO.service[WeatherProgram].provideSome(WeatherProgram.configuredLayer)
    val calendarProgram: ZIO.service[CalendarProgram].provideSome(CalendarProgram.configuredLayer)

    (weatherProgram <&> calendarProgram).provide(
      Client.default,
      WeatherConfig.layer,
      AccuWeatherClient.layer,
      TemplateRenderer.layer,
      CalendarConfig.layer,
      CalDavClient.layer,
      CalendarRenderer.layer,
    )

