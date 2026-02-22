package is.valsk.trmnlhomescreen

import is.valsk.trmnlhomescreen.calendar.{CalDavClient, CalendarProgram, CalendarRenderer}
import is.valsk.trmnlhomescreen.hass.HassProgram
import is.valsk.trmnlhomescreen.hass.protocol.api.{AuthenticationHandler, ConnectHandler, HassResponseMessageHandler}
import is.valsk.trmnlhomescreen.hass.protocol.{ChannelHandler, ProtocolHandler, TextHandler, UnhandledMessageHandler}
import is.valsk.trmnlhomescreen.weather.{AccuWeatherClient, TemplateRenderer, WeatherProgram}
import zio.*
import zio.http.Client

object Main extends ZIOAppDefault:

  override def run: ZIO[Any, Any, Any] =
    val weatherProgram = ZIO.serviceWithZIO[WeatherProgram](_.run)
    val calendarProgram = ZIO.serviceWithZIO[CalendarProgram](_.run)
    val hassProgram = ZIO.serviceWithZIO[HassProgram](_.run)

    (weatherProgram <&> calendarProgram <&> hassProgram).provide(
      Client.default,
      AccuWeatherClient.configuredLayer,
      TemplateRenderer.configuredLayer,
      CalDavClient.configuredLayer,
      CalendarRenderer.configuredLayer,
      WeatherProgram.configuredLayer,
      CalendarProgram.configuredLayer,
      HassProgram.configuredLayer,
    )
