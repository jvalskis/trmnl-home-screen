package is.valsk.trmnlhomescreen

import is.valsk.trmnlhomescreen.calendar.{CalDavClient, CalendarProgram}
import is.valsk.trmnlhomescreen.weather.{AccuWeatherClient, WeatherProgram}
import is.valsk.trmnlhomescreen.homeassistant.HomeAssistantProgram
import is.valsk.trmnlhomescreen.trmnl.TrmnlClient
import zio.*
import zio.http.Client
import zio.logging.backend.SLF4J

object Main extends ZIOAppDefault:

  override val bootstrap: ZLayer[ZIOAppArgs, Any, Any] =
    Runtime.removeDefaultLoggers >>> SLF4J.slf4j

  override def run: ZIO[Any, Any, Any] =
    val program =
      ZIO.serviceWithZIO[WeatherProgram](_.run) <&>
        ZIO.serviceWithZIO[CalendarProgram](_.run) <&>
        ZIO.serviceWithZIO[HomeAssistantProgram](_.run) <&>
        ZIO.serviceWithZIO[RenderProgram](_.run)

    program.provide(
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
