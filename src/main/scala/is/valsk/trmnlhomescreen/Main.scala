package is.valsk.trmnlhomescreen

import is.valsk.trmnlhomescreen.calendar.{CalDavClient, CalendarProgram}
import is.valsk.trmnlhomescreen.weather.{AccuWeatherClient, WeatherProgram}
import is.valsk.trmnlhomescreen.homeassistant.HomeAssistantProgram
import is.valsk.trmnlhomescreen.trmnl.TrmnlClient
import zio.*
import zio.http.Client

object Main extends ZIOAppDefault:

  override def run: ZIO[Any, Any, Any] =
    val weatherProgram = ZIO.serviceWithZIO[WeatherProgram](_.run)
    val calendarProgram = ZIO.serviceWithZIO[CalendarProgram](_.run)
    val homeAssistantProgram = ZIO.serviceWithZIO[HomeAssistantProgram](_.run)
    val renderProgram = ZIO.serviceWithZIO[RenderProgram](_.run)

    (weatherProgram <&> calendarProgram <&> homeAssistantProgram <&> renderProgram).provide(
      Client.default,
      AccuWeatherClient.configuredLayer,
      CalDavClient.configuredLayer,
      ScreenStateRepository.layer,
      ScreenRenderer.configuredLayer,
      TrmnlClient.configuredLayer,
      RenderProgram.configuredLayer,
      WeatherProgram.configuredLayer,
      CalendarProgram.configuredLayer,
      HomeAssistantProgram.configuredLayer,
    )
