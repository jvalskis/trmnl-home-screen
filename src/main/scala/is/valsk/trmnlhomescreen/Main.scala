package is.valsk.trmnlhomescreen

import is.valsk.trmnlhomescreen.calendar.{CalDavClient, CalendarProgram, CalendarRenderer}
import is.valsk.trmnlhomescreen.weather.{AccuWeatherClient, TemplateRenderer, WeatherProgram}
import is.valsk.trmnlhomescreen.hass.{HomeAssistantProgram, HomeAssistantRenderer}
import zio.*
import zio.http.Client

object Main extends ZIOAppDefault:

  override def run: ZIO[Any, Any, Any] =
    val weatherProgram = ZIO.serviceWithZIO[WeatherProgram](_.run)
    val calendarProgram = ZIO.serviceWithZIO[CalendarProgram](_.run)
    val homeAssistantProgram = ZIO.serviceWithZIO[HomeAssistantProgram](_.run)

    (weatherProgram <&> calendarProgram <&> homeAssistantProgram).provide(
      Client.default,
      AccuWeatherClient.configuredLayer,
      TemplateRenderer.configuredLayer,
      CalDavClient.configuredLayer,
      CalendarRenderer.configuredLayer,
      WeatherProgram.configuredLayer,
      CalendarProgram.configuredLayer,
      HomeAssistantRenderer.configuredLayer,
      HomeAssistantProgram.configuredLayer,
    )
