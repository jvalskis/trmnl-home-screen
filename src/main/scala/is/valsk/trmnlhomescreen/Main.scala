package is.valsk.trmnlhomescreen

import is.valsk.trmnlhomescreen.calendar.{CalDavClient, CalendarProgram}
import is.valsk.trmnlhomescreen.weather.{AccuWeatherClient, WeatherProgram}
import is.valsk.trmnlhomescreen.homeassistant.HomeAssistantProgram
import is.valsk.trmnlhomescreen.trmnl.TrmnlClient
import zio.*
import zio.http.Client

object Main extends ZIOAppDefault:

  private def resolveLogLevel(level: String): UIO[LogLevel] =
    ZIO.fromOption(LogLevel.levels.find(_.label == level.toUpperCase))
      .orElse(ZIO.logWarning(s"Logging level '$level' could not be resolved. Defaulting to INFO").map(_ => LogLevel.Info))

  override def run: ZIO[Any, Any, Any] =
    val program = for
      loggingConfig <- ZIO.service[LoggingConfig]
      logLevel <- resolveLogLevel(loggingConfig.level)
      weatherProgram = ZIO.serviceWithZIO[WeatherProgram](_.run)
      calendarProgram = ZIO.serviceWithZIO[CalendarProgram](_.run)
      homeAssistantProgram = ZIO.serviceWithZIO[HomeAssistantProgram](_.run)
      renderProgram = ZIO.serviceWithZIO[RenderProgram](_.run)
      _ <- ZIO.logLevel(logLevel) {
        weatherProgram <&> calendarProgram <&> homeAssistantProgram <&> renderProgram
      }
    yield ()

    program.provide(
      Client.default,
      LoggingConfig.layer,
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
