package is.valsk.trmnlhomescreen

import is.valsk.trmnlhomescreen.calendar.{CalDavClient, CalendarProgram, CalendarPropertiesExtractor, CalendarStateRepository}
import is.valsk.trmnlhomescreen.weather.{AccuWeatherClient, WeatherProgram, WeatherPropertiesExtractor, WeatherStateRepository}
import is.valsk.trmnlhomescreen.homeassistant.{HomeAssistantProgram, HomeAssistantPropertiesExtractor, HomeAssistantStateRepository}
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
      WeatherStateRepository.layer,
      CalendarStateRepository.layer,
      HomeAssistantStateRepository.layer,
      extractorsLayer,
      ScreenRenderer.configuredLayer,
      TrmnlClient.configuredLayer,
      RenderProgram.configuredLayer,
      WeatherProgram.configuredLayer,
      CalendarProgram.configuredLayer,
      HomeAssistantProgram.configuredLayer,
    )

  private val extractorsLayer: ZLayer[
    CalendarStateRepository & WeatherStateRepository & HomeAssistantStateRepository,
    Throwable,
    List[PropertiesExtractor],
  ] = ZLayer.scoped {
    for
      calendarExtractor <- CalendarPropertiesExtractor.configuredLayer.build.map(_.get[PropertiesExtractor])
      weatherExtractor <- WeatherPropertiesExtractor.configuredLayer.build.map(_.get[PropertiesExtractor])
      homeAssistantExtractor <- HomeAssistantPropertiesExtractor.configuredLayer.build.map(_.get[PropertiesExtractor])
    yield List(calendarExtractor, weatherExtractor, homeAssistantExtractor)
  }
